package ralph;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.atomic.AtomicInteger;

import RalphExceptions.BackoutException;
import RalphDataWrappers.DataWrapper;


/**
   ==Deadlock prevention invariant==
   A root object can try to acquire locks of speculative children
   while holding its own.  A speculative child cannot make a call
   to a root object that acquires root's lock while holding its
   own locks.

   ==Handling backouts==

   Naive approach
   ---------------
   Assume we have a chain of speculative objects:

   A -> B -> C
   
   If B backs out a write lock holder, then at end of backout it 1)
   releases its lock on itself and 2) requests A to invalidate C.  If,
   before A processes this request, A tries to promote speculateds, it
   will promote B (preventing C from being invalidated).  Further, if
   B now has no read or write lock holders, it could even promote to
   C, which should have been invalidated.


   Actual approach
   ---------------
   
   Each derivative object maintains an integer reference counter,
   derived_backout_derived_ref_count, and a condition variable,
   derived_condition, based on its lock.  Each time an object requests
   to backout derived objects, it increments this reference count,
   while holding its own lock.  When the root object processes this
   request, it:

      1) Acquires its own lock (which guards speculated_entries)
      2) Runs through speculated_entries invalidating objects
      3) Releases its own lock
      4) Acquires derived requester's lock
      5) Decrements the requester's reference count
      6) Signals on derived requester's condition variable
      7) Releases derived requester's reference count.
      

   When we try to promote and object to root (in promote speculated)
   then, we:


      1)   Acquire root's lock.
      1.5) Check if can promote speculated.
      2)   Acquire eldest derivative's lock.
      3)   Check if eldest derivative's reference counter is zero.
           If it is, goto 6.
      4)   Eldest derivative's reference counter is not zero.  Release
           root's lock.  Release eldest derivative's lock.  Listen for
           signal on eldest derivative's condition variable.
      5)   On signal, release eldest_derivative's lock and goto 1.
           (Important to go to 1 instead of just acquiring root's
           lock so that can maintain deadlock prevention invariant
           that cannot acquire root's _mutex while holding lock on
           derivative's.
      6)   transfer_to_root on eldest derivative to root.  Check if
           there are other objects to transfer.

    Note that once we get to try_promoted, nothing can invalidate and
    remove eldest derivative.  This means that eventually its
    reference counter must return to 0.
      

 */
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
       Note: both of these are only used by derivative objects.
       (derived_condition is set in set_speculative.)  See notes at
       top of file about when/how used.

       derived_backout_derived_ref_count can only be modified while
       holding object mutex.
     */
    private Condition derived_condition = null;
    private int derived_backout_derived_ref_count = 0;
    
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

    /**
       Only used by root objects. Keeps track of all outstanding
       requests that have been made to the root to commit its changes.

       We keep track of thsese outstanding speculatives because an
       AtomicActiveEvent can tell us to backout before the future has
       been assigned.  In these cases, we must wake up any thread that
       had been waiting on the future so that it can keep running.
     */
    private Map<String,ICancellableFuture> root_outstanding_commit_requests =
        new HashMap<String,ICancellableFuture>();

    
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
        derived_condition = _mutex.newCondition();
    }

    /**
       Can only be called on derived (non-root) objects.

       Can be called from within or without locks.
     */
    private void increment_backout_derived_ref_count()
    {
        //// DEBUG
        if (root_speculative)
        {
            Util.logger_assert(
                "Can only call increment_backout_derived_ref_count on " +
                "derived objects, not root.");
        }
        //// END DEBUG
        
        _lock();
        ++derived_backout_derived_ref_count;
        _unlock();
    }

    /**
       Can only be called on derived (non-root) objects.

       Can be called from within or without locks.

       Also signals if ref count goes to 0.
     */
    private void decrement_backout_derived_ref_count()
    {
        //// DEBUG
        if (root_speculative)
        {
            Util.logger_assert(
                "Can only call decrement_backout_derived_ref_count on " +
                "derived objects, not root.");
        }
        //// END DEBUG

        _lock();
        --derived_backout_derived_ref_count;
        if (derived_backout_derived_ref_count == 0)
            derived_condition.signalAll();

        //// DEBUG
        if (derived_backout_derived_ref_count < 0)
            Util.logger_assert("Ref count should never be < 0.");
        //// END DEBUG
        
        _unlock();
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


                * Note: we also do the same for all read lock holders,
                * except for the active event that is requesting
                * speculation.  We do this to handle the following
                * situation:

                     SpecObj alpha;
                     Event A, B;

                     A r_locks alpha;
                     B r_locks alpha;

                     B tries to w_lock alpha.  B is put into alpha's
                     wait list.

                     A speculates alpha -> alpha'
                         * B is still an rlock holder of alpha
                         * B becomes a wlock holder of alpha' when it
                           is unwaited in alpha'

                     The next time B tries to write, it acquires
                     alpha's wlock as well because it has already been
                     processed by alpha (ie, it's still in alpha's
                     readset).  Now B is a write lock holder on both
                     alpha and alpha'.  This is bad, because alpha'
                     must wait for alpha before it can complete its
                     commit.

              * Solution, for now, is to copy read set out of
              * speculated object.  Note: this interferes with
              * fairness guarantees.  Should think about fixing this.


     @param {T} to_speculate_on --- Can be null, in which case,
     speculate on existing value associated with active_event.

     @returns --- Returns the dirty value associated with
     active_event.  Note, if speculate is called on object that has no
     read lock holders, write lock holders, or waiting events (ie,
     this active event and no other have ever interacted with object),
     then return null.
     */
    public T speculate(ActiveEvent active_event, T to_speculate_on)
    {
        _lock();

        // No event to speculate on top of: do nothing.  Handles case
        // of what happens if speculate gets called on an object that
        // has never been a read lock holder.
        int root_num_outstanding =
            waiting_events.size() + read_lock_holders.size() +
            root_outstanding_commit_requests.size();
        if (root_num_outstanding == 0)
        {
            _unlock();
            return null;
        }
        
        T to_return = null;
        // Step 0: find associated to_speculate_on
        // FIXME: is this safe?

        ArrayList<SpeculativeAtomicObject<T,D>> to_iter_over =
            new ArrayList(speculated_entries);
        to_iter_over.add(0,this);
            
        for (SpeculativeAtomicObject<T,D> spec_obj : to_iter_over)
        {
            try
            {
                spec_obj._lock();
                if (spec_obj.read_lock_holders.containsKey(active_event.uuid))
                {
                    if ((spec_obj.write_lock_holder != null) &&
                        (spec_obj.write_lock_holder.event.uuid.equals(active_event.uuid)))
                    {
                        to_return = spec_obj.dirty_val.val;
                        break;
                    }
                        
                    to_return = spec_obj.val.val;
                    break;
                }
            }
            finally
            {
                spec_obj._unlock();
            }
        }

        if (to_speculate_on == null)
        {
            if (to_return != null)
                to_speculate_on = to_return;
            else if (dirty_val != null)
                to_speculate_on = dirty_val.val;
            else
                to_speculate_on = val.val;
        }
                
        // step 1
        SpeculativeAtomicObject<T,D> to_speculate_on_wrapper =
            duplicate_for_speculation(to_speculate_on);

        // step 2
        speculated_entries.add(to_speculate_on_wrapper);

        // step 3
        Map<String,WaitingElement<T,D>> prev_waiting_elements =
            null;
        Map<String,EventCachedPriorityObj> prev_read_events = null;
        if (speculating_on == null)
        {
            // means that we should grab waiting elements from this
            // object's waiting list. (and roll them over to new one)
            prev_waiting_elements = get_and_clear_waiting_events();
            prev_read_events =
                get_and_clear_alternate_read_events(active_event);
        }
        else
        {
            // means that we should grab waiting elements from
            // speculating_on's waiting_list.  (and roll them over to
            // new one)
            prev_waiting_elements =
                speculating_on.get_and_clear_waiting_events();
            prev_read_events =
                speculating_on.get_and_clear_alternate_read_events(active_event);
        }

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

            // we stop the speculating_on object from receiving
            // commands from the event (eg., backout, commit, etc.).
            // This is because from here on out the derived object
            // will receive commands and can decide whether or not to
            // forward them on to root.  If we did not remove
            // speculating_on from act_event's touched objects, then
            // speculating_on might receive duplicate commands (one
            // from its derived object and one directly).
            if (speculating_on != null)
                act_event.remove_touched_obj(speculating_on);
            else
                act_event.remove_touched_obj(this);
        }
        
        for (EventCachedPriorityObj read_lock_holder :
                 prev_read_events.values())
        {
            ActiveEvent act_event = read_lock_holder.event;
            if (! to_speculate_on_wrapper.insert_in_touched_objs(act_event))
                aborted_events.add(act_event);
            else
                schedule_try_next = true;

            // see comment in previous for loop.
            if (speculating_on != null)
                act_event.remove_touched_obj(speculating_on);
            else
                act_event.remove_touched_obj(this);
        }

        // note that it is important for this call to happen after
        // above aborted sort out.  This is because insert_in_touched
        // checks if an event is already waiting or a read lock holder
        // before telling the object to insert into its map of touched
        // objects
        to_speculate_on_wrapper.set_waiting_and_read_events(
            prev_waiting_elements,prev_read_events);

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
        return to_speculate_on;
    }

    /**
       Called from within lock.
       
       When a derived object gets promoted to root object, we need to
       deal with any events that began committing to the object when
       it was a derived object.

       Absent hardware specific logic for dealing with these (eg.,
       pushing to switches, etc.), we just say that the
       speculative_succeeded.
     */
    final protected void internal_first_phase_commit_speculative(
        SpeculativeFuture sf)
    {
        if (! hardware_first_phase_commit_speculative_hook(sf))
            sf.succeeded();
    }

    /**
       Note: generally, subclasses should not override this.  They
       should override internal_first_phase_commit and
       internal_first_phase_commit_speculative instead.

       After calling first_phase_commit, while still waiting on the
       future, an event may call backout on this object.  In that
       case, this object is responsible for ensuring that the future
       unblocks any listeners.

       An event that is not a reader or writer on this object can call
       this method.  This can happen if an event *had been* a
       read/writer on this object is backed out by one thread, but is
       still trying to commit to its touched objects on another.
       
       In this case, this method should return a future that will
       instantly return False.
     */
    public ICancellableFuture first_phase_commit(ActiveEvent active_event)
    {
        _lock();

        // check if running first because a derivative object that is
        // not running may not have an up to date read/write set.
        if ((speculation_state == SpeculationState.RUNNING) &&
            // cannot be a write lock holder without being a read lock
            // holder, therefore this check serves for both.
            (! read_lock_holders.containsKey(active_event.uuid)))
        {
            _unlock();
            return ALWAYS_FALSE_FUTURE;
        }
        
        // This is the base element that is trying to commit.  Just
        // try to commit normally.
        if (root_speculative)
        {
            ICancellableFuture to_return =
                hardware_first_phase_commit_hook(active_event);
            if (to_return == null)
                to_return = ALWAYS_TRUE_FUTURE;
            root_outstanding_commit_requests.put(active_event.uuid,to_return);
            _unlock();
            return to_return;
        }

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
            ICancellableFuture to_return =
                root_object.first_phase_commit(active_event);
            return to_return;
        }
        
        if (speculation_state == SpeculationState.RUNNING)
        {
            // wait on objects that we are deriving from before trying
            // to commit.  Note, while committing, the event still
            // holds read and read/write locks on
            SpeculativeFuture to_return = new SpeculativeFuture(active_event);
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
        // do not need to call hardware backout because only roots of
        // speculatives get pushed to hardware.
        _lock();
        // change speculation state to failed: any future event that
        // tries to commit will be backed out.
        speculation_state = SpeculationState.FAILED;        

        // run through all events that did try to commit to this
        // speculative object and tell them that they will need to
        // backout.
        for (ICancellableFuture sf : outstanding_commit_requests.values())
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
        boolean should_try_promote_speculated = false;
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
        

        ICancellableFuture to_cancel =
            root_outstanding_commit_requests.remove(active_event.uuid);
        if (to_cancel != null)
        {
            // User of this future should not rely on this future's being
            // loaded with failed, only that it unblocks any waiting
            // readers.
            to_cancel.failed();
        }
        if (outstanding_commit_requests != null)
        {
            // means that this is a derivative object.  Must fail its
            // own outstanding commit request, because internal
            // backout and backout_derived_from will only backout
            // subsequently derived objects.
            to_cancel = outstanding_commit_requests.remove(active_event.uuid);
            if (to_cancel != null)
                to_cancel.failed();
        }
        
        boolean write_backed_out = internal_backout(active_event);
        if (write_backed_out)
        {
            // If an object holding a write lock is backed out of a
            // speculative, we need to backout all speculatives
            // derived from that object.  If we are not the root,
            // then, we ask the root to perform this operation for us
            // (body of first if).  Otherwise, we are the root and
            // invalidate all derived objects.
            if (! root_speculative)
            {
                // note: we unlock first to maintain invariant that
                // cannot hold speculative lock and then lock root (to
                // prevent deadlock).  To prevent this object from
                // being promoted to root before backout_derived_from
                // takes effect, we increment
                // derived_backout_derived_ref_count.  When root runs
                // try_promote_speculated, if this reference count is
                // non-zero, waits until it becomes zero before
                // promoting.
                increment_backout_derived_ref_count();
                _unlock();
                root_object.backout_derived_from(this);
                try_next();
                // return statement here so that do not duplicate
                // unlock at end of method.
                return;
            }
            else
            {
                // Note: we must invalidate derivative objects so that
                // derived do not use incorrect, speculated on value.
                // only has an effect if this is a root object.
                root_invalidate_all_derivative_objects();
            }
        }
        else
        {
            if (root_speculative)
            {
                // we do not need to invalidate derivative objects,
                // because they speculated on top of the actual current
                // value.  But we should promote the eldest speculative
                // object to root so that it can continue.
                should_try_promote_speculated = true;
            }
            
            // note: we do not need to do anything in the else
            // condition here.  Assume we have a derivative chain A ->
            // B -> C, where A is the root.  Then if B gets backed
            // out, if only a single event is holding a read lock on B
            // and that event got backed out (in this method), there's
            // no way that any other events will reach B.  We could
            // either kill it here, or just let A skip over it when it
            // is promoting.  We do the second.
        }
        _unlock();
        
        if (should_try_promote_speculated)
            try_promote_speculated();
        
        try_next();
    }


    /**
       Should only be called on root object.

       Called from outside of lock.

       to_backout is a speculative object that held a write lock and
       was backed out of.  We need to tell all the speculative objects
       that were derived from to_backout that they are no longer valid.

       Note that because of asynchrony in call to
       backout_derived_from, we are not guaranteed that to_backout is
       still in our list of derivative objects.  Similarly, we may end
       up rolling back derivative objects that derived from the
       correct value of a derivative object.

       FIXME: check if the above policy could result in livelock.
     */
    private void backout_derived_from(SpeculativeAtomicObject<T,D> to_backout)
    {
        //// DEBUG: should only be called on root object
        if (! root_speculative)
        {
            Util.logger_assert(
                "Should only call backout_derived_from on root.");
        }   
        //// END DEBUG
        _lock();
        for (int i =0; i < speculated_entries.size(); ++i)
        {
            SpeculativeAtomicObject<T,D> spec_obj = speculated_entries.get(i);
            if (spec_obj == to_backout)
            {
                // remove all derivatives that were based on this
                // object.
                root_invalidate_derivative_objects(i+1);
                break;
            }
        }
        _unlock();

        // also signals if ref count goes to 0.  That way, waiter in
        // promote_speculated receives call.
        to_backout.decrement_backout_derived_ref_count();
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

        //// DEBUG
        if (speculation_state != SpeculationState.RUNNING)
        {
            // Should only allow transferring to root at most once.
            // (Ie, cannot already be in succeeded state.)  Should not
            // transfer a failed to root.
            Util.logger_assert(
                "Error: can only transfer_to_root from RUNNING state.");
        }
        //// END DEBUG

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


        // Once a derived object transfers its events (lock holders
        // and waiters) to root, root manages them explicitly.
        // Derived object no longer needs them, and therefore create
        // empty replicas of them so that root and derived do not
        // inadvertently perform concurrent accesses on them.  This
        // takes care of a series of concurrent access bugs that I've
        // noticed.  For instance, scheduling a try_next, being
        // transferred to root and then root an try_next operating on
        // state simultaneously now goes away.
        read_lock_holders = new HashMap<String,EventCachedPriorityObj>();
        write_lock_holder = null;
        waiting_events = new HashMap<String,WaitingElement<T,D>>();
        
        _unlock();
        
        return outstanding_commit_requests;
    }


    /**
       Called when an event with uuid "uuid" is promoted to boosted
       with priority "priority"
    */
    @Override
    public final void update_event_priority(String uuid, String new_priority)
    {
        _lock();
        // note: doing nothing if this is a derivative object that
        // has succeeded or failed.  If the derivative object has
        // succeeded, then root is now managing its read and write
        // lock holders as well as its waiting events queue.
        if (speculation_state == SpeculationState.SUCCEEDED)
        {
            // unlocking here so that maintaining invariant that child
            // cannot call into root without first releasing its lock.
            _unlock();

            // can produce duplicate calls into update_event_priority.
            // This is okay: will update cached priority with same
            // value.
            root_object.update_event_priority(uuid,new_priority);
            return;
        }
            
        if (speculation_state == SpeculationState.FAILED)
        {
            // nothing to do here.  We've already failed.
            _unlock();
            return;
        }
            
        boolean may_require_update =
            internal_update_event_priority(uuid,new_priority);
        _unlock();

        if (may_require_update)
            schedule_try_next();
    }
    

    @Override
    public void complete_commit(ActiveEvent active_event)
    {
        boolean should_try_next = false;
        _lock();
        
        if (speculation_state == SpeculationState.SUCCEEDED)
        {
            // unlocking here so that maintaining invariant that child
            // cannot call into root without first releasing its lock.
            _unlock();
            root_object.complete_commit(active_event);
            return;
        }
        else if (speculation_state == SpeculationState.FAILED)
        {
            Util.logger_assert(
                "Error should never be completing a commit " +
                "from a failed speculation state.");
        }
        else
        {
            internal_complete_commit(active_event);
            should_try_next = true;
        }

        if (root_speculative)
        {
            // Note that we do not need to actually fail or succeed
            // this ICancellableFuture because to get into
            // complete_commit, the future must have already been
            // succeeded.  Removing it now simply for garbage
            // collection.
            root_outstanding_commit_requests.remove(active_event.uuid);
        }

        
        _unlock();
        //# FIXME: may want to actually check whether the change could
        //# have caused another read/write to be scheduled.
        if (should_try_next)
        {
            // must call try_promote_speculated from outside of lock.
            try_promote_speculated();
            try_next();
        }
    }

    /**
       !!!Must be called from OUTSIDE of lock!!!
       
       Should only be called on root and by root.

       When we try to promote and object to root (in promote speculated)
       then, we:

          1)   Acquire root's lock.
          1.5) Check if can promote speculated.
          2)   Acquire eldest derivative's lock.
          3)   Check if eldest derivative's reference counter is zero.
               If it is, goto 6.
          4)   Eldest derivative's reference counter is not zero.  Release
               root's lock.  Release eldest derivative's lock.  Listen for
               signal on eldest derivative's condition variable.
          5)   On signal, release eldest_derivative's lock and goto 1.
               (Important to go to 1 instead of just acquiring root's
               lock so that can maintain deadlock prevention invariant
               that cannot acquire root's _mutex while holding lock on
               derivative's.
          6)   transfer_to_root on eldest derivative to root.  Check if
               there are other objects to transfer.
     */
    private void try_promote_speculated()
    {        
        //// DEBUG
        if (! root_speculative)
            Util.logger_assert("Can only call promote on root object.");
        //// END DEBUG

        // 1 from above
        _lock();

        //// DEBUG
        if (_lock_hold_count() != 1)
        {
            // it is important that we are not currently holding lock
            // on _mutex when enter try_speculated because we need
            // other threads to be able to perform operations on root
            // in case a derived object's backout ref count != 0.
            Util.logger_assert(
                "Can only call try_promote_speculated when not holding lock.");
        }
        //// END DEBUG
        
        // 1.5 check to see if we should promote any object that we
        // speculated from this root object
        if (read_lock_holders.isEmpty())
        {
            while (! speculated_entries.isEmpty())
            {
                SpeculativeAtomicObject<T,D> eldest_spec =
                    speculated_entries.get(0);

                // 2 from above
                eldest_spec._lock();
                // 3 from above
                if (eldest_spec.derived_backout_derived_ref_count != 0)
                {
                    // 4 from above
                    _unlock();
                    try
                    {
                        eldest_spec.derived_condition.await();
                    }
                    catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                        Util.logger_assert(
                            "Error: Disallowed interrupted exception " +
                            "occurred when waiting on ref count." );
                    }

                    //5 from above
                    eldest_spec._unlock();
                    try_promote_speculated();
                    return;
                }

                // 6 from above: proceed with actually removing
                // entries.

                // actually remove the entry
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

                // there can be cases where a derived object does not
                // hold any read or write locks.  (eg., a read lock
                // holder holds a lock on it, then gets backed out.)
                // In these cases, no other event can access these
                // speculative objects and we should just pass through
                // them.
                if ( (!waiting_on_commit.isEmpty()) ||
                     (!read_lock_holders.isEmpty()) ||
                     (!waiting_events.isEmpty()))
                {
                    break;
                }
            }
        }
        _unlock();
    }
    
    /**
       Speculative objects should be able to get and set waiting
       events on one of their sub-objects (ie, objects that we're
       using for speculation).  This is because we need to roll
       waiting events over from one transaction to the next.
     */
    protected Map<String,WaitingElement<T,D>>
        get_and_clear_waiting_events()
    {
        Map<String, WaitingElement<T,D>> to_return;
        _lock();
        to_return = waiting_events;
        waiting_events = new HashMap<String,WaitingElement<T,D>>();
        _unlock();
        return to_return;
    }

    /**
       When start speculating on an object, create a new object that
       has value to speculate on top of.  Copy over read set of the
       initial object, except for the active event in that object's
       read set that requested speculation.
     */
    protected Map<String, EventCachedPriorityObj> 
        get_and_clear_alternate_read_events(ActiveEvent active_event)
    {
        Map<String, EventCachedPriorityObj> to_return;
        _lock();
        to_return = read_lock_holders;
        read_lock_holders = new HashMap<String,EventCachedPriorityObj>();

        EventCachedPriorityObj ecpo =
            to_return.remove(active_event.uuid);
        if (ecpo != null)
            read_lock_holders.put(active_event.uuid,ecpo);

        _unlock();
        return to_return;
    }

    protected void set_waiting_and_read_events(
        Map<String,WaitingElement<T,D>> waiting_events,
        Map<String,EventCachedPriorityObj> read_lock_holders)
    {
        _lock();
        this.waiting_events = waiting_events;
        this.read_lock_holders = read_lock_holders;
        _unlock();
    }
    
    /**
       If speculating, re-map get_val to get from the object that
       we're speculating on instead.
     */
    protected DataWrapper<T,D> acquire_read_lock(ActiveEvent active_event)
        throws BackoutException
    {
        _lock();
        try
        {
            boolean already_processing = already_processing_event(
                active_event);

            if (already_processing)
                return super.internal_acquire_read_lock(active_event,_mutex);

            for (SpeculativeAtomicObject<T,D> derivative_object :
                     speculated_entries)
            {
                already_processing =
                    derivative_object.already_processing_event(active_event);
                if (already_processing)
                    return  derivative_object.internal_acquire_read_lock(active_event,_mutex);
            }

            // if we got here, it means that this object and none
            // of its derivatives have serviced active event before
            if (speculating_on != null)
                return speculating_on.internal_acquire_read_lock(active_event,_mutex);
            else
            {
                DataWrapper<T,D> to_return = super.internal_acquire_read_lock(active_event,_mutex);
                return to_return;
            }
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
       Assumes already within lock.
       
       @see root_invalidate_derivative_objects.
     */
    private void root_invalidate_all_derivative_objects()
    {
        root_invalidate_derivative_objects(0);
    }
    
    /**
       Root_Invalidate all derived objects from
       index_to_root_invalidate_from (inclusive) onwards.  Generally,
       when a speculated write backs out, it issues a sequence of
       calls that backs out all of the direved objects after it.  This
       means that the speculated write itself must do its ownl cleanup
       (eg., of outstanding_commit_requests.).
       
       Assumes already within lock.
     */
    protected void root_invalidate_derivative_objects(
        int index_to_invalidate_from)
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

    protected DataWrapper<T,D> acquire_write_lock(ActiveEvent active_event)
        throws BackoutException
    {
        _lock();
        try
        {
            // an event can still make local accesses to a variable
            // even after it requests us to speculate.  First check if
            // we should be acquiring on root object
            boolean already_processing = already_processing_event(
                active_event);

            if (already_processing)
            {
                // root object already had record of active event:
                // invalidate any derivative objects because we
                // performed the acquire on it.
                root_invalidate_all_derivative_objects();
                return super.internal_acquire_write_lock(active_event,_mutex);
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
                    root_invalidate_derivative_objects(i+1);
                    return derivative_object.internal_acquire_write_lock(
                        active_event,_mutex);
                }
            }
            
            // none of the objects that we were speculating on had
            // interacted with that event before.
            if (speculating_on != null)
            {
                return speculating_on.internal_acquire_write_lock(
                    active_event,_mutex);
            }
            else
            {
                // note: do not need to invalidate anything
                // because cannot get to else of this if-else
                // block unless we aren't speculating on any
                // objects.
                return super.internal_acquire_write_lock(active_event,_mutex);
            }
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
    }

    /**
       Assumes already within lock.  Gets called any time that this
       object releases itself from an event that was holding **because
       another event tries to assume a read or read/write lock on this
       object.  Note that because this is the case, there can be no
       derived objects and we therefore do not need to invalidate any
       derived objects.

       Note: we do not need to invalidate any derivative objects in
       this method.  This is because the only way that we can reach
       here is if we go through acquire_read_lock or
       acquire_write_lock.  acquire_write_lock takes care of
       invalidating derivative objects for us.

       @see documentation of overridden method.
     */
    @Override
    final protected void obj_request_backout_and_release_lock(
        ActiveEvent active_event)
    {
        if (root_speculative)
        {
            ICancellableFuture to_cancel =
                root_outstanding_commit_requests.remove(active_event.uuid);
            if (to_cancel != null)
                to_cancel.failed();
        }
        else
        {
            SpeculativeFuture to_cancel =
                outstanding_commit_requests.remove(active_event.uuid);
            if (to_cancel != null)
                to_cancel.failed();
        }
        super.internal_obj_request_backout_and_release_lock(active_event);
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
    protected static class FutureAlwaysValue implements ICancellableFuture
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

        @Override
        public void failed()
        {}

        @Override
        public void succeeded()
        {}

        @Override
        public Boolean get()
        {
            return what_to_return;
        }

        @Override
        public Boolean get(long timeout, TimeUnit unit)
        {
            return get();
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public boolean isCancelled()
        {
            Util.logger_assert(
                "FutureAlways does not support isCancelled");
            return false;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            Util.logger_assert(
                "FutureAlways does not support cancel");
            return false;
        }
    }
    
    public static final FutureAlwaysValue ALWAYS_TRUE_FUTURE =
        new FutureAlwaysValue(true);
    public static final FutureAlwaysValue ALWAYS_FALSE_FUTURE =
        new FutureAlwaysValue(false);
}
