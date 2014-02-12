package ralph;

import RalphExceptions.BackoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

public abstract class SpeculativeAtomicObject<T,D> extends AtomicObject<T,D>
{
    /**
       0th index is eldest element that we are speculating on.  Last
       index is most recent.
     */
    private List<SpeculativeAtomicObject<T,D>> speculated_entries =
        new ArrayList<SpeculativeAtomicObject<T,D>>();
    private SpeculativeAtomicObject<T,D> speculating_on = null;

    /**
       Derived speculative atomic objects cannot commit until their
       predecessors do.  Therefore, if a derived speculative object
       receives a request to commit from an event that is holding read
       or read/write locks on it, we store the future response in this
       map.  We do this so that if the speculative object we are
       deriving from cannot commit, we can notify all the Futures to
       return False, ie, that the commit could not take effect.  If
       the root speculativeatomicobjects could commit, then we try to
       apply the changes of this speculativeatomicobject and continue.

       Note: this map is only necessary for derived objects.
       Therefore, we only initialize it in set_derived.
     */
    private Map<String,SpeculativeFuture> outstanding_commit_requests = null;
    private SpeculationState speculation_state = SpeculationState.RUNNING;

    private enum SpeculationState
    {
        RUNNING,  // objects derived from are still running
        FAILED,   // one of speculatives we were speculating on failed.
        SUCCEEDED // all objects we derived from commit successfully.
    }

    /**
       Root object will always be null for speculative objects that
       are not derived from any other object.  All derived speculative
       objects point to their root object.  Value gets set in
       set_derived.
     */
    private SpeculativeAtomicObject<T,D> root_object = null;

    
    /**
       If speculative object B derives from speculative object A (ie,
       B is speculating on one of A's dirty values), then B should not
       commit before A is done.  Therefore we distinugish root
       speculatives (A) from derived speculatives (B) to keep track of
       whether a SpeculativeObject can commit when asked.

       Can change to derived via set_derived, which should be called
       almost immediately after duplicate_for_speculation.
     */
    private boolean root_speculative = true;
    
    public SpeculativeAtomicObject(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
    }

    /**
       See note above root_speculative.  Should be called immediately
       after duplicate_for_speculation.
     */
    protected void set_derived(SpeculativeAtomicObject<T,D> root_object)
    {
        this.root_object = root_object;
        root_speculative = false;
        outstanding_commit_requests = new HashMap<String,SpeculativeFuture> ();
    }
    
    protected abstract SpeculativeAtomicObject<T,D>
        duplicate_for_speculation(T to_speculate_on);
    
    /**
       When we speculate, we do three things:

         1) Create a new speculative atomic object to speculate on
            that uses passed in to_speculate_on as basis.
       
         2) We add a speculation entry to our list of speculation
            entries.  (And set speculating_on if the list had
            previously been empty.)

         3) Transfer all elements that are waiting on a read or
            read/write lock on this atomic object to instead try to
            acquire locks on the newly-created object.  We have to be
            careful about how we do this:

                Grab all waiting_events from previous object and
                remove waiting_event from previous object.  (Note:
                this means that we may get backout requests for events
                that we neither hold locks for nor are in our waiting
                element set.  This is fine, we can drop those.)

                Insert all waiting_events into new speculative
                object's waiting_event map.  Do not yet allow the
                speculative object to try servicing them.  This is
                because although the new speculative objects have a
                record of the waiting elements, the speculative
                objects themselves have not been added to each event's
                list of touched objects.  Must add to list of touched
                objects first so that if the event subsequently backs
                out the new speculative object will be notified.

                Next step: for all waiting events, insert the new
                speculative object into the waiting event's list of
                touched objects.  Note: trying to insert can fail.  In
                these cases, it means that the event is aborting.  We
                can unjam the waiting element's queue and remove the
                waiting element from the map on the speculative
                object.
            
     */
    public void speculate(T to_speculate_on)
    {
        _lock();

        // step 1
        SpeculativeAtomicObject<T,D> to_speculate_on_wrapper =
            duplicate_for_speculation(to_speculate_on);

        // step 2
        speculated_entries.add(to_speculate_on_wrapper);

        // step 3
        HashMap<String,WaitingElement<T,D>> prev_waiting_elements =
            null;
        if (speculating_on == null)
        {
            //means that we should grab waiting elements from this
            //object's waiting list. (and roll them over to new one)
            prev_waiting_elements = get_and_clear_waiting_events();
        }
        else
        {
            //means that we should grab waiting elements from
            // speculating_on's waiting_list.  (and roll them over to
            // new one)
            prev_waiting_elements =
                speculating_on.get_and_clear_waiting_events();
        }
        to_speculate_on_wrapper.set_waiting_events(prev_waiting_elements);
        
        Set<ActiveEvent> aborted_events = new HashSet<ActiveEvent>();
        boolean schedule_try_next = false;
        for (WaitingElement<T,D> waiting_element :
                 prev_waiting_elements.values())
        {
            ActiveEvent act_event = waiting_element.event;
            if (! to_speculate_on_wrapper.insert_in_touched_objs(act_event))
                aborted_events.add(act_event);
            else
                schedule_try_next = true;
        }

        // these events were already aborted, but this object has not
        // yet received a backout request for them.  This object will
        // eventually, but because we removed the event from this
        // object's waiting elements map, we won't be able to unjam
        // its waiting queue, and we must therefore do it expleicitly
        // here.
        for (ActiveEvent aborted : aborted_events)
            to_speculate_on_wrapper.internal_backout(aborted);
            
        
        // FIXME: should we just try unblocking directly here instead
        // of scheduling the try next.
        speculating_on = to_speculate_on_wrapper;
        if (schedule_try_next)
            speculating_on.schedule_try_next();

        _unlock();
    }

    /**
       Called from outside of lock.
       
       @returns {boolean} --- True if can commit in first phase, false
       otherwise.  Note: this method is really only useful for objects
       that push their changes to other hardware, (eg., a list that
       propagates its changes to a router).  If the hardware is still
       up and running, and the changes have been pushed, then, return
       true, otherwise, return false.
     */
    protected Future<Boolean> internal_first_phase_commit(
        ActiveEvent active_event)
    {
        return ALWAYS_TRUE_FUTURE;
    }
    protected void internal_first_phase_commit_speculative(
        SpeculativeFuture sf)
    {
        sf.succeeded();
    }

    /**
       Note: generally, subclasses should not override this.  They
       should override internal_first_phase_commit and
       internal_first_phase_commit_speculative instead.
     */
    public Future<Boolean> first_phase_commit(ActiveEvent active_event)
    {
        // This is the base element that is trying to commit.  Just
        // try to commit normally.
        if (root_speculative)
            return internal_first_phase_commit(active_event);

        _lock();
        if (speculation_state == SpeculationState.FAILED)
        {
            // means that one of the objects that we were speculating
            // on failed/was backed out.  Any event that operated on
            // values derived from the event that failed should not be
            // able to commit.
            _unlock();
            return ALWAYS_FALSE_FUTURE;
        }

        if (speculation_state == SpeculationState.SUCCEEDED)
        {
            // all derived objects succeeded.  We already updated
            // the root object so that it would get a notification
            // to also receive first_phase_commit from committing
            // active event.  Rely on that one to sort things out.
            _unlock();
            return root_object.first_phase_commit(active_event);
        }

        if (speculation_state == SpeculationState.RUNNING)
        {
            // wait on objects that we are deriving from before trying to
            // commit.
            SpeculativeFuture to_return = new SpeculativeFuture();
            outstanding_commit_requests.put(active_event.uuid,to_return);
            _unlock();
            return to_return;
        }


        Util.logger_assert(
            "Unknown speculation state in first_phase_commit.");
        return null;
    }

    /**
       One of the objects that this speculativeatomicobject derived
       from failed.  Must unroll all objects that speculated.
     */
    protected void derived_from_failed()
    {
        _lock();
        // change speculation state to failed: any future event that
        // tries to commit will be backed out.
        speculation_state = SpeculationState.FAILED;        

        // run through all events that did try to commit to this
        // speculative object and tell them that they will need to
        // backout.
        for (SpeculativeFuture sf : outstanding_commit_requests.values())
            sf.failed();
        outstanding_commit_requests.clear();
        
        _unlock();

        for (EventCachedPriorityObj cached_priority_obj :
                 read_lock_holders.values())
        {
            cached_priority_obj.event.backout(null,false);
        }

        for (WaitingElement<T,D> we : waiting_events.values())
        {
            we.event.backout(null,false);
            we.unwait(this);
        }
    }

    /**
       Only wrinkle is that if the objects this object derived from
       succeeded, we should forward all messages (in this case,
       backout) to root.
     */
    @Override
    public void backout (ActiveEvent active_event)
    {
        _lock();

        if (speculation_state == SpeculationState.SUCCEEDED)
        {
            // root_object should be in charge of handling backouts.
            _unlock();
            root_object.backout(active_event);
            return;
        }

        if (speculation_state == SpeculationState.FAILED)
        {
            // do not have to back anything out because this object
            // already has gone through backout.
            _unlock();
            return;
        }

        internal_backout(active_event);
        _unlock();
        try_next();
    }

    
    /**
       Should only be called by root_object.
       
       All of the objects that this object derived from succeeded in
       pushing their commits.  We should transfer all of our relevant
       internal state to the root object.

       Note: Assumes already holding lock on root_object.

       @returns --- Either null or a map.  If a map, the values of the
       map contain references to ActiveEvents that have attempted to
       commit themselves and are waiting on a response to finish
       commit.
     */
    protected Map<String,SpeculativeFuture> transfer_to_root()
    {
        _lock();

        // in this speculation state, all requests to this object will
        // now be forwarded on to root object, who will deal with
        // them.
        speculation_state = SpeculationState.SUCCEEDED;

        // copy all outstanding state to root_object.  this object
        // forwards any messages from ActiveEvent (eg., commit,
        // backout, etc.) to root.
        root_object.read_lock_holders = read_lock_holders;
        root_object.write_lock_holder = write_lock_holder;
        root_object.waiting_events = waiting_events;
        root_object.dirty_val = dirty_val;

        _unlock();
        
        return outstanding_commit_requests;
    }

    @Override
    public void complete_commit(ActiveEvent active_event)
    {
        _lock();
        internal_complete_commit(active_event);
        try_promote_speculated();

        if (speculation_state == SpeculationState.SUCCEEDED)
            root_object.complete_commit(active_event);
        
        _unlock();
        //# FIXME: may want to actually check whether the change could
        //# have caused another read/write to be scheduled.
        try_next();
        
    }

    /**
       Assumes already holding lock
     */
    private void try_promote_speculated()
    {
        // check to see if we should promote any object that we
        // speculated from this root object
        if (read_lock_holders.isEmpty())
        {
            if (! speculated_entries.isEmpty())
            {
                SpeculativeAtomicObject<T,D> eldest_spec =
                    speculated_entries.remove(0);
                
                // if the eldest entry was also the only entry, then
                // we should no longer be speculating.
                if (speculated_entries.isEmpty())
                    speculating_on = null;
                else
                {
                    speculating_on =
                        speculated_entries.get(speculated_entries.size() -1);
                }
                Map<String,SpeculativeFuture> waiting_on_commit =
                    eldest_spec.transfer_to_root();

                for (SpeculativeFuture sf : waiting_on_commit.values())
                    internal_first_phase_commit_speculative(sf);
            }
        }
    }

    
    /**
       Speculative objects should be able to get and set waiting
       events on one of their sub-objects (ie, objects that we're
       using for speculation).  This is because we need to roll
       waiting events over from one transaction to the next.
     */
    protected HashMap<String,WaitingElement<T,D>>
        get_and_clear_waiting_events()
    {
        HashMap<String, WaitingElement<T,D>> to_return;
        _lock();
        to_return = waiting_events;
        waiting_events = new HashMap<String,WaitingElement<T,D>>();
        _unlock();
        return to_return;
    }
    
    protected void set_waiting_events(
        HashMap<String,WaitingElement<T,D>> waiting_events)
    {
        _lock();
        this.waiting_events = waiting_events;
        _unlock();
    }
    

    /**
       If speculating, re-map get_val to get from the object that
       we're speculating on instead.
     */
    @Override
    public T get_val(ActiveEvent active_event) throws BackoutException
    {
        T to_return = null;
        _lock();
        try
        {
            boolean already_processing = already_processing_event(
                active_event);

            if (already_processing)
                return super.get_val(active_event,_mutex);

            for (SpeculativeAtomicObject<T,D> derivative_object :
                     speculated_entries)
            {
                already_processing =
                    derivative_object.already_processing_event(active_event);
                if (already_processing)
                    return  derivative_object.get_val(active_event,_mutex);
            }

            // if we got here, it means that this object and none
            // of its derivatives have serviced active event before
            if (speculating_on != null)
                return speculating_on.get_val(active_event,_mutex);
            else
                return super.get_val(active_event,_mutex);
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
    }

    /**
       Basic idea: if a speculative object had already been servicing
       an active event, we want to continue to permit it to service
       that active event.  This method checks if this object had
       already serviced the active event.
     */
    protected boolean already_processing_event(ActiveEvent active_event)
    {
        boolean already_processing_event = false;
        _lock();
        if (read_lock_holders.containsKey(active_event.uuid) ||
            waiting_events.containsKey(active_event.uuid))
            already_processing_event = true;
        _unlock();

        return already_processing_event;
    }
    

    /**
       Invalidate all derived objects from index_to_invalidate_from
       (inclusive) on.
       
       Assumes already within lock.
     */
    protected void invalidate_derivative_objects(int index_to_invalidate_from)
    {
        int end_index = speculated_entries.size();
        int num_invalidations_to_perform =
            end_index - index_to_invalidate_from;
        
        for (int i = 0; i < num_invalidations_to_perform; ++i)
        {
            SpeculativeAtomicObject<T,D> spec_on =
                speculated_entries.get(index_to_invalidate_from);
            spec_on.derived_from_failed();
            speculated_entries.remove(index_to_invalidate_from);
        }

        if (index_to_invalidate_from == 0)
            speculating_on = null;
        else
        {
            speculating_on =
                speculated_entries.get(speculated_entries.size() - 1);
        }
    }

    @Override
    public void set_val(ActiveEvent active_event,T new_val)
        throws BackoutException
    {
        _lock();
        try
        {
            // an event can still make local accesses to a variable
            // even after it requests us to speculate.  First check if
            // we should be performing the set against our root object
            boolean already_processing = already_processing_event(
                active_event);

            if (already_processing)
            {
                // root object already had record of active event:
                // invalidate any derivative objects because we
                // performed the set on it.
                invalidate_derivative_objects(0);
                super.set_val(active_event,new_val,_mutex);
                return;
            }

            // check if any of our derivative objects were already
            // handling this event.
            for (int i = 0; i < speculated_entries.size(); ++i)
            {
                SpeculativeAtomicObject<T,D> derivative_object =
                    speculated_entries.get(i);
                
                already_processing =
                    derivative_object.already_processing_event(
                        active_event);

                if (already_processing)
                {
                    //we should invalidate all the subsequently
                    //derived objects and break out of loop
                    invalidate_derivative_objects(i+1);
                    derivative_object.set_val(active_event,new_val,_mutex);
                    return;                        
                }
            }
            
            // none of the objects that we were speculating on had
            // interacted with that event before.
            if (speculating_on != null)
                speculating_on.set_val(active_event,new_val,_mutex);
            else
            {
                // note: do not need to invalidate anything
                // because cannot get to else of this if-else
                // block unless we aren't speculating on any
                // objects.
                super.set_val(active_event,new_val,_mutex);
            }
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
    }

    protected class SpeculativeFuture implements Future<Boolean>
    {
        private final ReentrantLock rlock = new ReentrantLock();
        private final Condition cond = rlock.newCondition();
        private boolean has_been_set = false;
        private boolean to_return = false;
        
        public void failed()
        {
            set(false);
        }

        public void succeeded()
        {
            set(true);
        }
        
        private void set(boolean to_set_to)
        {
            rlock.lock();
            has_been_set = true;
            to_return = to_set_to;
            cond.signalAll();
            rlock.unlock();
        }

        @Override
        public Boolean get()
        {
            rlock.lock();
            while(! has_been_set)
            {
                try
                {
                    cond.await();
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "\nShould be handling interrupted exception\n");
                }
            }
            rlock.unlock();
            return to_return;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit)
        {
            Util.logger_assert(
                "SpeculativeFuture does not support timed gets");
            return null;
        }

        @Override
        public boolean isCancelled()
        {
            Util.logger_assert(
                "SpeculativeFuture does not support isCancelled");
            return false;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            Util.logger_assert(
                "SpeculativeFuture does not support cancel");
            return false;
        }
        
        @Override
        public boolean isDone()
        {
            boolean to_return;
            rlock.lock();
            to_return = has_been_set;
            rlock.unlock();
            return to_return;
        }        
    }

    /**
       AtomicActiveEvents request AtomicObjects to enter first phase
       commit.  This triggers atomic objects to perform any work they
       need to ensure their changes are valid/can be pushed.

       Return a future, which the AtomicActiveEvent can check to
       ensure that the change went through.  The future below will
       always return true.  Subclasses, when they override this object
       may override to use a different future that actually does work.
     */
    protected static class FutureAlwaysValue implements Future<Boolean>
    {
        private static final Boolean TRUE_BOOLEAN = new Boolean(true);
        private static final Boolean FALSE_BOOLEAN = new Boolean(false);

        private Boolean what_to_return = null;
        public FutureAlwaysValue(boolean _what_to_return)
        {
            if (_what_to_return)
                what_to_return = TRUE_BOOLEAN;
            else
                what_to_return = FALSE_BOOLEAN;
        }
            
        public Boolean get()
        {
            return what_to_return;
        }

        public Boolean get(long timeout, TimeUnit unit)
        {
            return get();
        }

 	public boolean isCancelled()
        {
            return false;
        }
        public boolean isDone()
        {
            return true;
        }
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return false;
        }
    }
    protected static final FutureAlwaysValue ALWAYS_TRUE_FUTURE =
        new FutureAlwaysValue(true);
    protected static final FutureAlwaysValue ALWAYS_FALSE_FUTURE =
        new FutureAlwaysValue(false);
}
