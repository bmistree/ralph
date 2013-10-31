package RalphServiceActions;

import java.util.concurrent.ArrayBlockingQueue;

import ralph.ActiveEventMap;
import ralph.ExecutingEventContext;
import ralph.ExecutingEvent;
import ralph.LockedActiveEvent;
import ralph.VariableStore;

/**
Another endpoint has asked us to execute an action on our own
endpoint as part of a global event.
*/
public class ReceiveEndpointCallAction extends ServiceAction
{
    private ralph.Endpoint local_endpoint = null;
    private ralph.Endpoint endpoint_making_call = null;
    String event_uuid;
    String priority;
    String func_name;
    ArrayBlockingQueue<RalphCallResults.EndpointCallResultObject> result_queue;
    Object [] args = null;

	
    /**
     * @param {_Endpoint object} local_endpoint --- The endpoint
     which was requested to backout.  (Ie, the endpoint on which
     this action will take place.)

     For other @params, @see, _EndpointServiceThread._endpointCall.
    */
    public ReceiveEndpointCallAction(
        ralph.Endpoint _local_endpoint, ralph.Endpoint _endpoint_making_call,
        String _event_uuid, String _priority, String _func_name,
        ArrayBlockingQueue<RalphCallResults.EndpointCallResultObject> _result_queue,
        Object [] _args)
    {
        local_endpoint = _local_endpoint;
        endpoint_making_call = _endpoint_making_call;
        event_uuid = _event_uuid;
        priority = _priority;
        func_name = _func_name;
        result_queue = _result_queue;
        args = _args;
    }
	
    @Override
    public void run() 
    {
        ActiveEventMap act_evt_map = local_endpoint._act_event_map;
        LockedActiveEvent act_event = null;
        try
        {
            act_event = act_evt_map.get_or_create_endpoint_called_event(
                endpoint_making_call,event_uuid, priority, result_queue);
        }
        catch(RalphExceptions.StoppedException ex)
        {
            result_queue.add(
                new RalphCallResults.StopAlreadyCalledEndpointCallResult());
            return;
        }

        ExecutingEventContext evt_ctx = new ExecutingEventContext(
            // will fork the stack in executing event context
            local_endpoint.global_var_stack);

	    
        //# receiving endpoint must know that this call was an endpoint
        //# call.  This is so that it can ensure to make deep copies of
        //# all non-external arguments (including lists,maps, and user
        //# structs).
        evt_ctx.set_from_endpoint_true();
        String to_exec_internal_name = ralph.Util.endpoint_call_func_name(func_name);
	    		
        ExecutingEvent.static_run(
            to_exec_internal_name, act_event, evt_ctx, result_queue, true, args);
    }

}
