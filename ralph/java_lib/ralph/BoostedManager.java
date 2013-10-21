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
    private ArrayList<LockedActiveEvent> event_list =
        new ArrayList<LockedActiveEvent>();
	
	
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
	
    public LockedActiveEvent create_root_event()
    {
        String evt_uuid = Util.generate_uuid();
        
        String evt_priority;
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

        RootEventParent rep = 
            new RootEventParent(
                act_event_map.local_endpoint,evt_uuid,evt_priority);
        
        LockedActiveEvent root_event = new LockedActiveEvent(rep,act_event_map);
        event_list.add(root_event);
        return root_event;
    }
	
    /**
       @param {UUID} completed_event_uuid

       @param {bool} retry --- If the event that we are removing is
       being removed because backout was called, we want to generate
       a new event with a successor UUID (the same high-level bits
       that control priority/precedence, but different low-level
       version bits)
        
       Whenever any root event completes, this method gets called.
       If this event had been a boosted event, then we boost the next
       waiting event.  Otherwise, we remove it from the list of
       waiting uuids.

       @returns {null/LockedActiveEvent} --- If retry is True, we
       return a new event with successor uuid.  Otherwise, return
       null. 
    */
    public LockedActiveEvent complete_root_event(
        String completed_event_uuid, boolean retry)
    {
        int counter = 0;
        int remove_counter = -1;
        LockedActiveEvent completed_event = null;
        for (LockedActiveEvent event : event_list)
        {
            if (event.uuid == completed_event_uuid)
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

        LockedActiveEvent replacement_event = null;
        if (retry)
        {
            String replacement_priority = "";
            /*
              in certain cases, we do not actually promote each
              event's priority to boosted.  For instance, if the event
              is already in process of committing.  However, if that
              commit goes awry and we backout, we want the replacement
              event generated to use a boosted event priority, rather
              than its original priority.
            */
            if (counter == 0)
            {
                // new event should be boosted.
            	
                if( ! EventPriority.is_boosted_priority(completed_event.get_priority()))
                {
                    /* 
                     * if it wasn't already boosted, that means that we
                     tried to promote it while it was in the midst of
                     its commit and we ignored the promotion.
                     Therefore, we want to apply the promotion on
                     retry.
                    */
                    replacement_priority =
                        EventPriority.generate_boosted_priority(last_boosted_complete);
                }
                else
                {
                    // it was already boosted, just reuse it
                    replacement_priority = completed_event.get_priority();
                }
            }
            else
            {
                // it was not boosted, just increment the version number
                replacement_priority = completed_event.get_priority();
            }

            RootEventParent rep = new RootEventParent(
                act_event_map.local_endpoint,Util.generate_uuid(),
                replacement_priority);
            
            replacement_event = new LockedActiveEvent(rep,act_event_map);
            event_list.set(counter, replacement_event);
        }
        else
        {
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
        return replacement_event;
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
