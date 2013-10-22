package ralph
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

import RalphExceptions.StoppedException;


public class ActiveEventMap
{
    private HashMap<String,LockedActiveEvent> map = new HashMap<String,LockedActiveEvent>();
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
        ArrayList<LockedActiveEvent> evt_list = new ArrayList<LockedActiveEvent>(map.values());
        
        in_stop_phase = true;
        _unlock();
        
        for (LockedActiveEvent evt : evt_list)
            evt.stop(skip_partner);        
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
    
    
    /**
     * Generates a new active event for events that were begun on
     this endpoint and returns it.        
     * @return
     */
    public LockedActiveEvent create_root_event() throws RalphExceptions.StoppedException
    {
        _lock();
        if (in_stop_phase)
        {
            _unlock();
            throw new RalphExceptions.StoppedException();
        }

        LockedActiveEvent root_event = boosted_manager.create_root_event();
        map.put(root_event.uuid,root_event);
        _unlock();
        return root_event;
    }
    
    /**
     * 
     * @param event_uuid
     * @param retry --- If retry is true, when we remove the
     event, we create another root event whose uuid is either:
     1) Boosted and/or
           
     2) Has the same highlevel bits in its uuid as the removed
     event.  (The high level bits hold the time stamp that
     the event began as well as whether the event is
     boosted.)

     This way, if we are removing an event because the event is
     retrying, the event will not lose its priority in the event
     map on retry.

     * 
     *  @returns --- @see remove_event_if_exists' return statement 
     */
    public ActiveEventTwoTuple remove_event(String event_uuid, boolean retry)
    {
        return remove_event_if_exists(event_uuid, retry);
    }

    /**
     * 
     * @param event_uuid 
     * @param retry --- @see remove_event
     * @return ----
     a {Event or None} --- If an event existed in the map, then
     we return it.  Otherwise, return None.

     b {Event or None} --- If we requested retry-ing, then
     return a new root event with
     successor uuid to event_uuid.
     Otherwise, return None.

    */
    public ActiveEventTwoTuple remove_event_if_exists(String event_uuid, boolean retry)
    {        
        _lock();
        
        LockedActiveEvent to_remove = map.remove(event_uuid);
        LockedActiveEvent successor_event = null;
        
        if ((to_remove != null) &&
            RootEventParent.class.isInstance(to_remove.event_parent))
        {
            successor_event = boosted_manager.complete_root_event(event_uuid,retry);
            if (successor_event != null)
                map.put(successor_event.uuid, successor_event);
        }

        boolean fire_stop_complete_callback = false;
        
        if ((map.isEmpty()) && (in_stop_complete_phase))
            fire_stop_complete_callback = true;
        
        _unlock();
        
        if (fire_stop_complete_callback)
            stop_callback.run();

        ActiveEventTwoTuple to_return = new ActiveEventTwoTuple(to_remove,successor_event);
        return to_return;
    }

    /**
     * @returns {None,_ActiveEvent} --- None if event with name uuid
     is not in map.  Otherwise, reutrns the _ActiveEvent in the
     map.
    */
    public LockedActiveEvent get_event(String uuid)
    {
        _lock();
        LockedActiveEvent to_return = map.get(uuid);
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
    public LockedActiveEvent get_or_create_partner_event(
        String uuid, String priority) throws StoppedException
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
                LockedActiveEvent new_event =
                    new LockedActiveEvent(pep,this);
                map.put(uuid, new_event);
            }
        }

        LockedActiveEvent to_return = map.get(uuid);
        _unlock();
        return to_return;
    }

    public ActiveEventTwoTuple remove_event(String uuid_to_remove)
    {
        return remove_event(uuid_to_remove,false);
    }

    /**
       @param {Endpoint object} endpoint --- The endpoint that made
       the endpoint call onto us.
       * @throws StoppedException 

       @see description above get_or_create_partner_event
    
       Create an event because received an endpoint call
    */
	
    public LockedActiveEvent get_or_create_endpoint_called_event(
        Endpoint endpoint_calling, String event_uuid, String priority,
        ArrayBlockingQueue<RalphCallResults.EndpointCallResultObject>result_queue)
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
            LockedActiveEvent new_event = new LockedActiveEvent(eep,this);
            map.put(event_uuid, new_event);
        }
        LockedActiveEvent to_return = map.get(event_uuid);
		
        _unlock();
        return to_return;
	
    }

}
