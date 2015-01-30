package ralph;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import RalphDurability.DurabilityContext;


public class ActiveEventMap
{
    private java.util.concurrent.locks.ReentrantLock _mutex = 
        new java.util.concurrent.locks.ReentrantLock();
    public Endpoint local_endpoint = null;
    private BoostedManager boosted_manager = null;
    private final RalphGlobals ralph_globals;
    
    public ActiveEventMap(
        Endpoint _local_endpoint, LamportClock clock,
        DeadlockAvoidanceAlgorithm daa,RalphGlobals _ralph_globals)
    {
        ralph_globals = _ralph_globals;
        local_endpoint = _local_endpoint;
        boosted_manager = new BoostedManager(this,clock,daa,_ralph_globals);
    }

    /**
       Using mutex so that can only create one event for a particular
       uuid.  As an example of what could go wrong if did not:

         1) Receive partner rpc request: start creating partner event
         
         2) While creating partner event, get a notification to
            backout partner.  (That event isn't yet in map, so do
            nothing.)
     */
    private void _lock()
    {    	
        _mutex.lock();
    }
    private void _unlock()
    {
        _mutex.unlock();
    }
    
    public void inform_events_of_network_failure()
    {
        Util.logger_assert(
            "\nMust fill in inform_events_of_network_failure " +
            "in active event map\n");
    }
    
    public AtomicActiveEvent create_root_atomic_event(
        ActiveEvent event_parent, Endpoint root_endpoint,
        String event_entry_point_name,DurabilityContext durability_context)
    {
        return (AtomicActiveEvent)create_root_event(
            true,event_parent,false,root_endpoint,
            event_entry_point_name,durability_context);
    }

    public NonAtomicActiveEvent create_root_non_atomic_event(
        Endpoint root_endpoint, String event_entry_point_name)
    {
        return (NonAtomicActiveEvent)create_root_event(
            false,null,false,root_endpoint,event_entry_point_name,null);
    }
    
    /**
       Can only create supers for non atomic events.  Further, cannot
       change from being super to anything else.  Cannot change from
       anything else to becoming super.
     */
    public NonAtomicActiveEvent create_super_root_non_atomic_event(
        Endpoint root_endpoint, String event_entry_point_name)
    {
        return (NonAtomicActiveEvent)create_root_event(
            false,null,true,root_endpoint, event_entry_point_name,null);
    }


    /**
       @param {boolean} atomic --- True if root event that we create
       should be atomic instead of non-atomic.  False for non-atomics.

       @param{boolean} super_priority --- True if should create the
       event to have a super priority.  Note: cannot create super
       atomic root events directly through this interface.  Atomics
       can only inherit super priority from their super non-atomic
       parents.
       
       Generates a new active event for events that were begun on this
       endpoint and returns it.
     */
    private ActiveEvent create_root_event(
        boolean atomic, ActiveEvent event_parent, boolean super_priority,
        Endpoint root_endpoint, String event_entry_point_name,
        DurabilityContext durability_context)
    {
        // DEBUG
        if (super_priority && atomic)
            Util.logger_assert(
                "Can only create non-atomic super root events.\n");
        // END DEBUG

        ActiveEvent root_event = null;
        if (atomic)
        {
            root_event =
                boosted_manager.create_root_atomic_event(
                    event_parent,root_endpoint,event_entry_point_name,
                    durability_context);
        }
        else
        {
            root_event =
                boosted_manager.create_root_non_atomic_event(
                    super_priority,root_endpoint,event_entry_point_name);
        }

        local_endpoint.ralph_globals.all_events.put(root_event.uuid,root_event);
        return root_event;
    }
    
    /**
     * 
     * @param event_uuid
     * 
     *  @returns --- @see remove_event_if_exists' return statement 
     */
    public ActiveEvent remove_event(String event_uuid)
    {
        return remove_event_if_exists(event_uuid);
    }

    /**
     * 
     * @param event_uuid 
     * @return ----
     a {Event or None} --- If an event existed in the map, then
     we return it.  Otherwise, return None.
    */
    public ActiveEvent remove_event_if_exists(String event_uuid)
    {        
        ActiveEvent to_remove =
            local_endpoint.ralph_globals.all_events.remove(event_uuid);
        
        if ((to_remove != null) && (to_remove.event_parent.is_root))
            boosted_manager.complete_root_event(event_uuid);

        return to_remove;
    }
    
    
    /**
     * @returns {None,_ActiveEvent} --- None if event with name uuid
     is not in map.  Otherwise, reutrns the _ActiveEvent in the
     map.
    */
    public ActiveEvent get_event(String uuid)
    {
        ActiveEvent to_return =
            local_endpoint.ralph_globals.all_events.get(uuid);
        return to_return;
    }

    /**
     * Get or create an event because partner endpoint requested it.

     @param event_entry_point_name --- If have to generate the partner
     event, then use this to label the entry point for the entry point
     name.
     
     @returns {_ActiveEvent}
    */
    public ActiveEvent get_or_create_partner_event(
        String uuid, String priority,boolean atomic,
        String event_entry_point_name)
    {
        _lock();

        ActiveEvent to_return = local_endpoint.ralph_globals.all_events.get(uuid);
        if (to_return == null)
        {
            PartnerEventParent pep =
                new PartnerEventParent(
                    local_endpoint._host_uuid,local_endpoint,uuid,priority,
                    ralph_globals,event_entry_point_name);
            ActiveEvent new_event = null;
            if (atomic)
            {
                DurabilityContext durability_context = null;
                if (DurabilityInfo.instance.durability_saver != null)
                {
                    durability_context =
                        new DurabilityContext(pep.get_uuid());
                }
                new_event = new AtomicActiveEvent(
                    pep,local_endpoint._thread_pool,this,null,ralph_globals,
                    durability_context);
            }
            else
                new_event = new NonAtomicActiveEvent(pep,this,ralph_globals);

            local_endpoint.ralph_globals.all_events.put(uuid,new_event);
            to_return = new_event;
        }
        _unlock();
        return to_return;        
    }
}
