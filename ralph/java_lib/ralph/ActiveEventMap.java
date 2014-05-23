package ralph;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import RalphExceptions.StoppedException;


public class ActiveEventMap
{
    private java.util.concurrent.locks.ReentrantLock _mutex = 
        new java.util.concurrent.locks.ReentrantLock();
    public Endpoint local_endpoint = null;
    private boolean in_stop_phase = false;
    private boolean in_stop_complete_phase = false;
    private StopCallback stop_callback = null;
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
    
    /**
     *  When the endpoint that this is on has said to start
     stopping, then
     * @param skip_partner
     */
    public void initiate_stop(boolean skip_partner)
    {
        Util.logger_assert("Stop has been deprecated");
    }

    public void inform_events_of_network_failure()
    {
        Util.logger_assert(
            "\nMust fill in inform_events_of_network_failure " +
            "in active event map\n");
    }
    
    public void callback_when_stopped(StopCallback stop_callback_)
    {
        Util.logger_assert("Stop has been deprecated");
    }
    

    public AtomicActiveEvent create_root_atomic_event(ActiveEvent event_parent)
        throws RalphExceptions.StoppedException
    {        
        return
            (AtomicActiveEvent)create_root_event(
                true,event_parent,false,new AtomicLogger());
    }

    public NonAtomicActiveEvent create_root_non_atomic_event()
        throws RalphExceptions.StoppedException
    {
        return
            (NonAtomicActiveEvent)create_root_event(
                false,null,false,new AtomicLogger());
    }
    
    /**
       Can only create supers for non atomic events.  Further, cannot
       change from being super to anything else.  Cannot change from
       anything else to becoming super.
     */
    public NonAtomicActiveEvent create_super_root_non_atomic_event()
        throws RalphExceptions.StoppedException
    {
        
        return
            (NonAtomicActiveEvent)create_root_event(
                false,null,true,new AtomicLogger());
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
        AtomicLogger atomic_logger)
        throws RalphExceptions.StoppedException
    {
        atomic_logger.log("c_rt_evt top");
        atomic_logger.log("c_rt_ac top");
        // DEBUG
        if (super_priority && atomic)
            Util.logger_assert(
                "Can only create non-atomic super root events.\n");
        // END DEBUG
        
        _lock();
        atomic_logger.log("c_rt_ac bottom");
        
        if (in_stop_phase)
        {
            _unlock();
            throw new RalphExceptions.StoppedException();
        }

        ActiveEvent root_event = null;
        if (atomic)
            root_event = boosted_manager.create_root_atomic_event(event_parent,atomic_logger);
        else
            root_event = boosted_manager.create_root_non_atomic_event(super_priority,atomic_logger);

        atomic_logger.log("glob_put_evt top");
        local_endpoint.ralph_globals.all_events.put(root_event.uuid,root_event);
        atomic_logger.log("glob_put_evt bottom");
        _unlock();
        atomic_logger.log("c_rt_evt bottom");
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

     b {Event or None} --- If we requested retry-ing, then
     return a new root event with
     successor uuid to event_uuid.
     Otherwise, return None.

    */
    public ActiveEvent remove_event_if_exists(String event_uuid)
    {
        AtomicLogger logger = new AtomicLogger();
        logger.log("re_ie top");
        logger.log("re_lock top");
        _lock();
        logger.log("re_bottom top");
        ActiveEvent to_remove = local_endpoint.ralph_globals.all_events.remove(event_uuid);
        ActiveEvent successor_event = null;

        if ((to_remove != null) &&
            RootEventParent.class.isInstance(to_remove.event_parent))
        {
            boosted_manager.complete_root_event(event_uuid,logger);
        }

        _unlock();
        logger.log("re_ie bottom");
        return to_remove;
    }

    
    /**
     * @returns {None,_ActiveEvent} --- None if event with name uuid
     is not in map.  Otherwise, reutrns the _ActiveEvent in the
     map.
    */
    public ActiveEvent get_event(String uuid)
    {
        _lock();
        ActiveEvent to_return =
            local_endpoint.ralph_globals.all_events.get(uuid);
        _unlock();
        return to_return;
    }

    /**
     * Get or create an event because partner endpoint requested it.
     Note: if we have to create an event and are in stop phase,
     then we throw a stopped exception.  If the event already
     exists though, we return it (this is so that we can finish any
     commits that we were waiting on).
     * @throws StoppedException 
        
     @returns {_ActiveEvent}
    */
    public ActiveEvent get_or_create_partner_event(
        String uuid, String priority,boolean atomic) throws StoppedException
    {
        _lock();

        ActiveEvent to_return = local_endpoint.ralph_globals.all_events.get(uuid);
        if (to_return == null)
        {
            if (in_stop_phase)
            {
                _unlock();
                throw new RalphExceptions.StoppedException();
            }
            else 
            {
                PartnerEventParent pep =
                    new PartnerEventParent(
                        local_endpoint._host_uuid,local_endpoint,uuid,priority,
                        ralph_globals);
                ActiveEvent new_event = null;
                if (atomic)
                {
                    new_event = new AtomicActiveEvent(
                        pep,local_endpoint._thread_pool,this,null);
                }
                else
                    new_event = new NonAtomicActiveEvent(pep,this);
                
                local_endpoint.ralph_globals.all_events.put(uuid,new_event);
                to_return = new_event;
            }
        }
        _unlock();
        return to_return;
    }
}
