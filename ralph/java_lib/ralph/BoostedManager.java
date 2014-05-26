package ralph;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
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

       Note that super events are not inserted into this list.  This
       is to prevent them from being promoted.
    */
    private ArrayList<ActiveEvent> event_list =
        new ArrayList<ActiveEvent>();

    // access is no longer protected by underlying activeeventmap,
    // must use own locking
    private ReentrantLock _mutex = new ReentrantLock();
    
    /**
       Allow programmer to choose type of deadlock avoidance algorithm
       to use.
     */
    public enum DeadlockAvoidanceAlgorithm {
        BOOSTED, WOUND_WAIT
    }

    private DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm;
    
    private final RalphGlobals ralph_globals;
    
    /**
     * @param _act_event_map
     * @param _clock --- Host's global clock.
     */
    public BoostedManager(
        ActiveEventMap _act_event_map, LamportClock _clock,
        DeadlockAvoidanceAlgorithm _deadlock_avoidance_algorithm,
        RalphGlobals _ralph_globals)
    {
        act_event_map = _act_event_map;
        clock = _clock;
        deadlock_avoidance_algorithm = _deadlock_avoidance_algorithm;
        last_boosted_complete = clock.get_timestamp();
        ralph_globals = _ralph_globals;
    }

    public ActiveEvent create_root_atomic_event(
        ActiveEvent atomic_parent,AtomicLogger logger)
    {
        return create_root_event(true,atomic_parent,false,logger);
    }

    /**
       @param {boolean} super_priority --- True if non atomic active
       event should have a super priority.
     */
    public ActiveEvent create_root_non_atomic_event(
        boolean super_priority,AtomicLogger logger)
    {
        return create_root_event(false,null,super_priority,logger);
    }

    private void _lock()
    {
        _mutex.lock();
    }
    private void _unlock()
    {
        _mutex.unlock();
    }
    

    /**
       @param {boolean} super_priority --- true if non atomic active
       event should have a super priority.  false otherwise.  Note:
       cannot create an atomic event directly with super priority.
       Atomics can only inherit super priority from their super
       non-atomic parents.  Will throw error if try to create atomic
       directly with super priority.
     */
    private ActiveEvent create_root_event(
        boolean atomic,ActiveEvent atomic_parent,boolean super_priority,
        AtomicLogger logger)
    {
        logger.log("b_c_evt top");
        // DEBUG
        if (super_priority && atomic)
            Util.logger_assert(
                "Can only create non-atomic super root events.\n");
        // END DEBUG

        logger.log("b_ce top");
        String evt_uuid = ralph_globals.generate_uuid();
        RootEventParent rep = 
            new RootEventParent(
                act_event_map.local_endpoint._host_uuid,evt_uuid,null,
                ralph_globals);

        ActiveEvent root_event = null;
        if (atomic)
        {
            root_event =
                new AtomicActiveEvent(
                    rep,
                    act_event_map.local_endpoint._thread_pool,
                    act_event_map,atomic_parent);
        }
        else
            root_event = new NonAtomicActiveEvent(rep,act_event_map);
        logger.log("b_ce bottom");

        logger.log("b_c_priority top");
        String evt_priority = null;
        logger.log("b_c_ac top");
        _lock();
        logger.log("b_c_ac bottom");
        
        if (atomic_parent != null)
        {
            // atomic should inherit parent's priority.
            evt_priority = atomic_parent.event_parent.get_priority();
        }
        else if (super_priority)
        {
            // super priority
            evt_priority =
                EventPriority.generate_super_priority(
                    clock.get_and_increment_timestamp());
        }
        else if (event_list.isEmpty())
        {
            if (deadlock_avoidance_algorithm == DeadlockAvoidanceAlgorithm.BOOSTED)
            {
                // boosted priority
                evt_priority = 
                    EventPriority.generate_boosted_priority(last_boosted_complete);
            }
            else if (deadlock_avoidance_algorithm == DeadlockAvoidanceAlgorithm.WOUND_WAIT)
            {
                evt_priority =
                    EventPriority.generate_standard_priority(
                        clock.get_and_increment_timestamp());
            }
            // DEBUG
            else
                Util.logger_assert("Unknown deadlock avoidance algo selected");
            // END DEBUG
        }
        else
        {
            // standard priority
            evt_priority =
                EventPriority.generate_standard_priority(
                    clock.get_and_increment_timestamp());
        }
        rep.initialize_priority(evt_priority);
        logger.log("b_c_priority bottom");
        // do not insert supers into event list: event list is a queue
        // that keeps track of which root event to promote to boosted.
        // We cannot boost supers, so do not insert it.
        if (!EventPriority.is_super_priority(evt_priority))
            event_list.add(root_event);
        _unlock();
        logger.log("b_c_evt bottom");
        return root_event;
    }
    
    /**
       @param {UUID} completed_event_uuid
        
       Whenever any root event completes, this method gets called.
       If this event had been a boosted event, then we boost the next
       waiting event.  Otherwise, we remove it from the list of
       waiting uuids.

    */
    public void complete_root_event(String completed_event_uuid,AtomicLogger logger)
    {
        logger.log("b_c_root top");
        int counter = 0;
        int remove_counter = -1;
        ActiveEvent completed_event = null;
        logger.log("b_c_ac top");
        _lock();
        logger.log("b_c_ac bottom");
        logger.log("b_list top");
        try
        {
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
            if (remove_counter == -1)
            {
                // note: not inserting super events into event_list.  This
                // is because we never want to promote them: supers are
                // always supers.  Therefore, may not have an event in
                // event list with completed_event_uuid if it's super.
                logger.log("b_list bottom");
                return;                
            }

            logger.log("b_list bottom");
            
            /*
              we are not retrying this event: remove the event from
              the list and if there are any other outstanding events,
              check if they should be promoted to boosted status.
            */
            event_list.remove(counter);
            if (counter == 0)
            {
                last_boosted_complete = clock.get_and_increment_timestamp();
                promote_first_to_boosted();
            }
        }
        finally
        {
            _unlock();
            logger.log("b_c_root bottom");
        }
    }

        
    /**
     * 
     MUST BE CALLED FROM WITHIN LOCK
     
     If there is an event in event_list, then turn that event into
     a boosted event.  If there is not, then there are no events to
     promote and we should do nothing.            
    */
    public void promote_first_to_boosted()
    {
        // perform no promotions if not using boosted.
        if (deadlock_avoidance_algorithm != DeadlockAvoidanceAlgorithm.BOOSTED)
            return;
        
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
