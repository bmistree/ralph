package ralph;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import RalphExceptions.StoppedException;


public class ActiveEventMap
{
    private HashMap<String,ActiveEvent> map = new HashMap<String,ActiveEvent>();
    private java.util.concurrent.locks.ReentrantLock _mutex = 
        new java.util.concurrent.locks.ReentrantLock();
    public Endpoint local_endpoint = null;
    private boolean in_stop_phase = false;
    private boolean in_stop_complete_phase = false;
    private StopCallback stop_callback = null;
    private BoostedManager boosted_manager = null;
    
    public ActiveEventMap(Endpoint _local_endpoint, LamportClock clock)
    {
        local_endpoint = _local_endpoint;
        boosted_manager = new BoostedManager(this, clock);    	
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
     *  When the endpoint that this is on has said to start
     stopping, then
     * @param skip_partner
     */
    public void initiate_stop(boolean skip_partner)
    {
        _lock();
        if (in_stop_phase)
        {
            // can happen if simultaneously attempt to stop connection
            // on both ends or if programmer calls stop twice.
            _unlock();
            return;
        }
        
        // note that when we stop events, they may try to remove
        // themselves from the map.  To prevent invalidating the map as
        // we iterate over it, we first copy all the elements into a
        // list, then iterate.
        ArrayList<ActiveEvent> evt_list =
            new ArrayList<ActiveEvent>(map.values());
        
        in_stop_phase = true;
        _unlock();
        
        for (ActiveEvent evt : evt_list)
            evt.stop(skip_partner);        
    }

    public void inform_events_of_network_failure()
    {
        Util.logger_assert(
            "\nMust fill in inform_events_of_network_failure " +
            "in active event map\n");
    }
    
    public void callback_when_stopped(StopCallback stop_callback_)
    {
        _lock();
        in_stop_complete_phase = true;
        stop_callback = stop_callback_;
        int len_map = map.size();
        _unlock();
        
        // handles case where we called stop when we had no outstanding events.
        if (len_map == 0)
            stop_callback.run();
    }
    

    public ActiveEvent create_root_atomic_event(ActiveEvent event_parent)
        throws RalphExceptions.StoppedException
    {
        return create_root_event(true,event_parent);
    }

    public ActiveEvent create_root_non_atomic_event()
        throws RalphExceptions.StoppedException
    {
        return create_root_event(false,null);
    }
    

    /**
       @param {boolean} atomic --- True if root event that we create
       should be atomic instead of non-atomic.  False for non-atomics.
       
       Generates a new active event for events that were begun on this
       endpoint and returns it.
     */
    private ActiveEvent create_root_event(
        boolean atomic, ActiveEvent event_parent)
        throws RalphExceptions.StoppedException
    {
        _lock();
        if (in_stop_phase)
        {
            _unlock();
            throw new RalphExceptions.StoppedException();
        }

        ActiveEvent root_event = null;
        if (atomic)
            root_event = boosted_manager.create_root_atomic_event(event_parent);
        else
            root_event = boosted_manager.create_root_non_atomic_event();

        map.put(root_event.uuid,root_event);
        _unlock();
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
        _lock();
        
        ActiveEvent to_remove = map.remove(event_uuid);
        ActiveEvent successor_event = null;
        
        if ((to_remove != null) &&
            RootEventParent.class.isInstance(to_remove.event_parent))
        {
            boosted_manager.complete_root_event(event_uuid);
        }

        boolean fire_stop_complete_callback = false;
        
        if ((map.isEmpty()) && (in_stop_complete_phase))
            fire_stop_complete_callback = true;
        
        _unlock();
        
        if (fire_stop_complete_callback)
            stop_callback.run();

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
        ActiveEvent to_return = map.get(uuid);
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
		
        if (! map.containsKey(uuid))
        {
            if (in_stop_phase)
            {
                _unlock();
                throw new RalphExceptions.StoppedException();
            }
            else 
            {
                PartnerEventParent pep =
                    new PartnerEventParent(local_endpoint,uuid,priority);
                ActiveEvent new_event = null;
                if (atomic)
                    new_event = new AtomicActiveEvent(pep,this,null);
                else
                    new_event = new NonAtomicActiveEvent(pep,this);
                map.put(uuid, new_event);
            }
        }

        ActiveEvent to_return = map.get(uuid);
        _unlock();
        return to_return;
    }

    /**
       @param {Endpoint object} endpoint --- The endpoint that made
       the endpoint call onto us.
       * @throws StoppedException 

       @see description above get_or_create_partner_event
    
       Create an event because received an endpoint call
    */
	
    public ActiveEvent get_or_create_endpoint_called_event(
        Endpoint endpoint_calling, String event_uuid, String priority,
        ArrayBlockingQueue<RalphCallResults.EndpointCallResultObject>result_queue,
        boolean atomic)
        throws StoppedException 
    {
        _lock();
        if (! map.containsKey(event_uuid))
        {
            if (in_stop_phase)
            {
                _unlock();
                throw new RalphExceptions.StoppedException();
            }

            
            EndpointEventParent eep =
                new EndpointEventParent(
                    event_uuid,endpoint_calling, local_endpoint,result_queue,priority);
            ActiveEvent new_event = null;
            if (atomic)
                new_event = new AtomicActiveEvent(eep,this,null);
            else
                new_event = new NonAtomicActiveEvent(eep,this);
            
            map.put(event_uuid, new_event);
        }
        ActiveEvent to_return = map.get(event_uuid);
		
        _unlock();
        return to_return;
	
    }

}
