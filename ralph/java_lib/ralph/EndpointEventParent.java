package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import WaldoCallResults.MessageCallResultObject;
import WaldoCallResults.EndpointCallResultObject;
import WaldoCallResults.NetworkFailureEndpointCallResult;

import WaldoCallResults.ApplicationExceptionEndpointCallResult;

public class EndpointEventParent extends EventParent
{

    waldo.Endpoint parent_endpoint = null;
    ArrayBlockingQueue<EndpointCallResultObject> result_queue = null;
	
    public EndpointEventParent(
        String _uuid,Endpoint _parent_endpoint,
        Endpoint _local_endpoint,
        ArrayBlockingQueue<EndpointCallResultObject>_result_queue,
        String _priority)
    {
        super(_local_endpoint,_uuid,_priority);
		
        parent_endpoint = _parent_endpoint;
        result_queue = _result_queue;
    }
	
    public void rollback(
        String backout_requester_endpoint_uuid, 
        HashMap<String,EventSubscribedTo> same_host_endpoints_contacted_dict,
        boolean partner_contacted, boolean stop_request)
    {
        HashMap<String,EventSubscribedTo> copy_other_endpoints_contacted = 
            (HashMap<String,EventSubscribedTo>)same_host_endpoints_contacted_dict.clone();
		
        copy_other_endpoints_contacted.put(
            parent_endpoint._uuid, 
            new EventSubscribedTo(parent_endpoint,result_queue));
	    
        super.rollback(
            backout_requester_endpoint_uuid,copy_other_endpoints_contacted,
            partner_contacted,stop_request);
    }
	    
    /**
       For arguments, @see EventParent.
    */
    public void first_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,
        boolean partner_contacted, LockedActiveEvent _event)
    {
        super.first_phase_transition_success(
            same_host_endpoints_contacted_dict,partner_contacted,event);

        //# tell parent endpoint that first phase has gone well and that
        //# it should wait on receiving responses from all the following
        //# endpoint uuids before entering second phase
        ArrayList<String> children_endpoints = 
            new ArrayList<String>(same_host_endpoints_contacted_dict.keySet());
        if (partner_contacted)
            children_endpoints.add(local_endpoint._partner_uuid);

        parent_endpoint._receive_first_phase_commit_successful(
            uuid,local_endpoint._uuid,children_endpoints);
    }

	
    /**
     * Places an ApplicationExceptionCallResult or NetworkFailureCallResult in 
     the event complete queue to indicate to the endpoint that an exception
     has been raised. This allows the exception to be propagated back.
    */
    @Override
    public void put_exception(
        Exception error,
        HashMap<String, ArrayBlockingQueue<MessageCallResultObject>> message_listening_queues_map) 
    {
        if (WaldoExceptions.NetworkException.class.isInstance(error))
        {
            result_queue.add(
                new NetworkFailureEndpointCallResult(error.toString()));
        }
        else if (WaldoExceptions.ApplicationException.class.isInstance(error))
        {
            result_queue.add(
                new ApplicationExceptionEndpointCallResult(error.toString()));
        }
        else
        {
            result_queue.add(
                new ApplicationExceptionEndpointCallResult(error.toString()));
        }
		
    }

    /**
       Forward message on to parent
    */
    @Override
    public void receive_successful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids) 
    {
        parent_endpoint._receive_first_phase_commit_successful(
            uuid,msg_originator_endpoint_uuid,children_event_endpoint_uuids);
    }
	
}


