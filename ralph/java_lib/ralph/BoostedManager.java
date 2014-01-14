package ralph;

import java.util.ArrayList;

import RalphServiceActions.ServiceAction;
import RalphServiceActions.PromoteBoostedAction;

public class BoostedManager
{
    private LamportClock clock = null;
    private String last_boosted_complete = null;
    private ActiveEventMap act_event_map = null;
    /**
       Each element is a root event.  the closer to zero index an
       event is in this list, the older it is.  The oldest event in
       this list should be made a boosted event.
    */
    private ArrayList<ActiveEvent> event_list =
        new ArrayList<ActiveEvent>();
	
	
    /**
     * @param _act_event_map
     * @param _clock --- Host's global clock.
     */
    public BoostedManager(ActiveEventMap _act_event_map, LamportClock _clock)
    {
        act_event_map = _act_event_map;
        clock = _clock;
        last_boosted_complete = clock.get_timestamp();		
    }

    public ActiveEvent create_root_atomic_event(ActiveEvent atomic_parent)
    {
        return create_root_event(true,atomic_parent);
    }

    public ActiveEvent create_root_non_atomic_event()
    {
        return create_root_event(false,null);
    }
    
    private ActiveEvent create_root_event(
        boolean atomic,ActiveEvent atomic_parent)
    {
        String evt_uuid = Util.generate_uuid();
        
        String evt_priority;

        if (atomic_parent != null)
        {
            // atomic should inherit parent's priority.
            evt_priority = atomic_parent.event_parent.get_priority();
        }
        else
        {
            if (event_list.isEmpty())
            {
                evt_priority =
                    EventPriority.generate_boosted_priority(last_boosted_complete);
            }
            else
            {
                evt_priority =
                    EventPriority.generate_timed_priority(
                        clock.get_and_increment_timestamp());
            }
        }

        RootEventParent rep = 
            new RootEventParent(
                act_event_map.local_endpoint,evt_uuid,evt_priority);
        
        ActiveEvent root_event = null;

        if (atomic)
            root_event = new AtomicActiveEvent(rep,act_event_map,atomic_parent);
        else
            root_event = new NonAtomicActiveEvent(rep,act_event_map);
        
        event_list.add(root_event);
        return root_event;
    }
	
    /**
       @param {UUID} completed_event_uuid
        
       Whenever any root event completes, this method gets called.
       If this event had been a boosted event, then we boost the next
       waiting event.  Otherwise, we remove it from the list of
       waiting uuids.

    */
    public void complete_root_event(String completed_event_uuid)
    {
        int counter = 0;
        int remove_counter = -1;
        ActiveEvent completed_event = null;
        for (ActiveEvent event : event_list)
        {
            if (event.uuid.equals(completed_event_uuid))
            {
                remove_counter = counter;
                completed_event = event;
                break;
            }
            counter += 1;
        }

        /// DEBUG
        if (remove_counter == -1)
        {
            Util.logger_assert(
                "Completing a root event that does not exist");
        }
        /// END DEBUG

        /*
          we are not retrying this event: remove the event from
          the list and if there are any other outstanding events,
          check if they should be promoted to boosted status.
        */
        event_list.remove(counter);
        if (counter == 0)
        {
            last_boosted_complete = clock.get_timestamp();
            promote_first_to_boosted();
        }
    }
    
        
    /**
     * 
     If there is an event in event_list, then turn that event into
     a boosted event.  If there is not, then there are no events to
     promote and we should do nothing.            
    */
    public void promote_first_to_boosted()
    {
    	if (event_list.isEmpty())
            return;
    	
        String boosted_priority =
            EventPriority.generate_boosted_priority(last_boosted_complete);
        ServiceAction service_action = new PromoteBoostedAction(
            event_list.get(0),boosted_priority);
        
        act_event_map.local_endpoint._thread_pool.add_service_action(
            service_action);
    }

}
