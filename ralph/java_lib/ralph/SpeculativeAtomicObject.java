package ralph;

import RalphExceptions.BackoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


public abstract class SpeculativeAtomicObject<T,D> extends AtomicObject<T,D>
{
    /**
       0th index is eldest element that we are speculating on.  Last
       index is most recent.
     */
    private List<SpeculativeAtomicObject<T,D>> speculated_entries =
        new ArrayList<SpeculativeAtomicObject<T,D>>();
    private SpeculativeAtomicObject<T,D> speculating_on = null;

    public SpeculativeAtomicObject(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
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
    protected void speculate(T to_speculate_on,String evt_waiting_on_uuid)
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
            if (speculating_on != null)
            {
                to_return =
                    speculating_on.get_val(active_event);
            }
            else
                to_return = super.get_val(active_event);
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
        finally
        {
            _unlock();
        }
        return to_return;
    }

    @Override
    public void set_val(ActiveEvent active_event,T new_val)
        throws BackoutException
    {
        _lock();
        try
        {
            if (speculating_on != null)
                speculating_on.set_val(active_event,new_val);
            else
                super.set_val(active_event,new_val);
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
        finally
        {
            _unlock();
        }
    }
}
    