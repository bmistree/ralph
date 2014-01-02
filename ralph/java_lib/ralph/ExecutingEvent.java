package ralph;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import RalphCallResults.EndpointCompleteCallResult;
import RalphCallResults.EndpointCallResultObject;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;


public class ExecutingEvent 
{

    private static Class[] param_types_args =
        {ActiveEvent.class, ExecutingEventContext.class, Object[].class};
    private static Class[] param_types_no_args =
        {ActiveEvent.class, ExecutingEventContext.class};
	
    private String to_exec_internal_name;
    private ActiveEvent active_event;
    private ExecutingEventContext ctx;
    private ArrayBlockingQueue<EndpointCallResultObject> result_queue;
    private Object[] to_exec_args;
    private boolean takes_args;
	
    /**
       @param {String} to_exec_internal_name --- The internal
       name of the method to execute on this endpoint. 
    

       @param {_ActiveEvent object} active_event --- The active event
       object that to_exec should use for accessing endpoint data.

       @param {_ExecutingEventContext} ctx ---

       @param {result_queue or null} --- This value should be
       non-null for endpoint-call initiated events.  For endpoint
       call events, we wait for the endpoint to check if any of the
       peered data that it modifies also need to be modified on the
       endpoint's partner (and wait for partner to respond).  (@see
       discussion in waldoActiveEvent.wait_if_modified_peered.)  When
       finished execution, put wrapped result in result_queue.  This
       way the endpoint call that is waiting on the result can
       receive it.  Can be None only for events that were initiated
       by messages (in which the modified peered data would already
       have been updated).
    
       @param {*args} to_exec_args ---- Any additional arguments that
       get passed to the closure to be executed.
    */
    public ExecutingEvent(
        String _to_exec_internal_name,ActiveEvent _active_event,
        ExecutingEventContext _ctx,
        ArrayBlockingQueue<EndpointCallResultObject> _result_queue,
        boolean _takes_args,// sequence calls do not take arguments
        Object..._to_exec_args)
    {
        to_exec_internal_name = _to_exec_internal_name;
        active_event = _active_event;
        takes_args = _takes_args;
        ctx = _ctx;
        result_queue = _result_queue;
        to_exec_args = _to_exec_args;
        
    }
	
    /**
     * @see arguments to constructor.
     */
    public static void static_run(
        String to_exec_internal_name,ActiveEvent active_event,
        ExecutingEventContext ctx,
        ArrayBlockingQueue<EndpointCallResultObject> result_queue,
        boolean takes_args, Object...to_exec_args)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException
    {
        Endpoint endpt_to_run_on = active_event.event_parent.local_endpoint;
        endpt_to_run_on.handle_rpc_call(
            to_exec_internal_name,active_event,
            ctx,result_queue,to_exec_args);
    }
			
    public void run()
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException
    {
        static_run(
            to_exec_internal_name,active_event,ctx,
            result_queue,takes_args,to_exec_args);
    }
		
}
