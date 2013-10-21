package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import WaldoCallResults.RootCallResultObject;
import java.util.concurrent.locks.ReentrantLock;
import WaldoCallResults.MessageCallResultObject;
import WaldoCallResults.NetworkFailureCallResult;

public class RootEventParent extends EventParent {

    /**
     * 	# indices are event uuids.  Values are bools.  When all values
     # are true in this dict, then we can transition into second
     # phase commit.
    */
    private HashMap<String,Boolean> endpoints_waiting_on_commit =
        new HashMap<String,Boolean>();
    
    /**
       # when the root tries to commit the event, it blocks while
       # reading the event_complete_queue
    */
    public ArrayBlockingQueue<RootCallResultObject>event_complete_queue = 
        new ArrayBlockingQueue<RootCallResultObject>(Util.SMALL_QUEUE_CAPACITIES);

    /**
       # we can add and remove events to waiting on commit lock from
       # multiple threads.  it appears that this can desynchronize
       # operations on endpoints_waiting_on_commit.  For instance, if
       # one thread puts in that it's waiting on a particular
       # endpoint and another thread puts in that the other
       # endpoint's first phase transition has been received, we can
       # get a case where the first message overwrites the operations
       # of the second.
    */
    private ReentrantLock _endpoints_waiting_on_commit_lock =
        new ReentrantLock();
	
    /**
       @param{uuid} uuid --- If None, then generate a random uuid and
       use that.  Otherwise, use the uuid specified.  Note: not all
       root events use a random uuid because some must be boosted.
    */
    public RootEventParent(
        Endpoint _local_endpoint,String _uuid, String _priority)
    {
        super(_local_endpoint,_uuid,_priority);
    }

    private void  _lock_endpoints_waiting_on_commit()
    {
        _endpoints_waiting_on_commit_lock.lock();
    }
    
    private void _unlock_endpoints_waiting_on_commit()
    {
        _endpoints_waiting_on_commit_lock.unlock();
    }

        
    /**
     * @see second_phase_transition_success in EventParent
     */
    public void second_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,    		
        boolean partner_contacted)
    {
    	event_complete_queue.add(
            new WaldoCallResults.CompleteRootCallResult());
    	super.second_phase_transition_success(
            same_host_endpoints_contacted_dict, partner_contacted);
    }

    /**
       Places the appropriate call result in the event complete queue to 
       indicate to the endpoint that an error has occured and the event
       must be handled.        
    */
    @Override
    public void put_exception(
        Exception error, 
        HashMap<String,ArrayBlockingQueue<MessageCallResultObject>> message_listening_queues_map)
    {
    	if (WaldoExceptions.NetworkException.class.isInstance(error))
    	{
            //# Send a NetworkFailureCallResult to each listening queue
            for (String reply_with_uuid : message_listening_queues_map.keySet())
            {
                ArrayBlockingQueue<MessageCallResultObject> message_listening_queue = 
                    message_listening_queues_map.get(reply_with_uuid);
                message_listening_queue.add(
                    new NetworkFailureCallResult(error.toString()));
            }
    	}
    }
    
    /**
     * For arguments, @see EventParent.
     */
    @Override
    public void first_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,
        boolean partner_contacted, LockedActiveEvent _event)
    {
        // # note that we should not wait on ourselves to commit
    	_lock_endpoints_waiting_on_commit();
    	endpoints_waiting_on_commit.put(local_endpoint._uuid, true);
    	
    	if (partner_contacted)
            endpoints_waiting_on_commit.put(local_endpoint._partner_uuid, false);
    		
    	for (String waiting_on_uuid : same_host_endpoints_contacted_dict.keySet())
            endpoints_waiting_on_commit.put(waiting_on_uuid, false);
    	
        //# not waiting on self.
        endpoints_waiting_on_commit.put(local_endpoint._uuid, true);
        _unlock_endpoints_waiting_on_commit();

        super.first_phase_transition_success(
            same_host_endpoints_contacted_dict, partner_contacted, _event);

        //# after first phase has completed, should check if can
        //# transition directly to second phase (ie, no other endpoints
        //# were involved in event.)
        check_transition();
    }

    /**
       If we are no longer waiting on any endpoint to acknowledge
       first phase commit, then transition into second phase commit.
    */
    public void check_transition()
    {
        _lock_endpoints_waiting_on_commit();
        for (Boolean endpoint_transitioned :
                 endpoints_waiting_on_commit.values())
        {
            if (! endpoint_transitioned.booleanValue())
            {
                _unlock_endpoints_waiting_on_commit();
                return;
            }
        }
            
        _unlock_endpoints_waiting_on_commit();
        event.second_phase_commit();
    }
	
    @Override
    public void rollback(
        String backout_requester_endpoint_uuid, 
        HashMap<String,EventSubscribedTo> same_host_endpoints_contacted_dict,
        boolean partner_contacted, boolean stop_request)
    {
        super.rollback(
            backout_requester_endpoint_uuid, same_host_endpoints_contacted_dict,
            partner_contacted,stop_request);
    

        //# put val to read into event_complete_queue so can know
        //# whether or not to retry event.
        
        WaldoCallResults.RootCallResultObject queue_feeder = null;
        if (stop_request)
            queue_feeder = new WaldoCallResults.StopRootCallResult();
        else
            queue_feeder = new WaldoCallResults.RescheduleRootCallResult();
        
        event_complete_queue.add(queue_feeder);
    }

    /**
       @see super class comments

       Update the list of endpoints that we are waiting on
       committing.  Check whether should transition into second phase
       of commit.
    */
    @Override
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids)
    {
    	_lock_endpoints_waiting_on_commit();
    	endpoints_waiting_on_commit.put(msg_originator_endpoint_uuid, true);
    	boolean may_transition = true;
    	for (String end_uuid : children_event_endpoint_uuids)
    	{
            Boolean val = endpoints_waiting_on_commit.get(end_uuid);
            if (val == null)
            {
                endpoints_waiting_on_commit.put(end_uuid, false);
                may_transition = false;
            }
            _unlock_endpoints_waiting_on_commit();
    	}
    	
    	if (may_transition)
            check_transition();
    }

}
