package ralph;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import ralph.MessageSender.IMessageSender;
import ralph.ExecutionContext.ExecutionContext;

public class ExecutingEvent 
{
    private final String to_exec_method_name;
    private final ExecutionContext exec_ctx;
    private final Object[] to_exec_args;
    private final boolean takes_args;
    private final Endpoint endpt_to_run_on;

    /**
       @param {String} to_exec_method_name --- The name of the method
       to execute on this endpoint.

       @param {ExecutionContext object} exec_ctx --- Contains the
       active_event object that to_exec should use for accessing
       endpoint data.
    
       @param {*args} to_exec_args ---- Any additional arguments that
       get passed to the closure to be executed.
    */
    public ExecutingEvent(
        Endpoint _endpt_to_run_on,
        String _to_exec_method_name, ExecutionContext _exec_ctx,
        boolean _takes_args,// sequence calls do not take arguments
        Object..._to_exec_args)
    {
        endpt_to_run_on = _endpt_to_run_on;
        to_exec_method_name = _to_exec_method_name;
        exec_ctx = _exec_ctx;
        takes_args = _takes_args;
        to_exec_args = _to_exec_args;
    }
	
    /**
     * @see arguments to constructor.
     */
    public static void static_run(
        Endpoint endpt_to_run_on,
        String to_exec_method_name, ExecutionContext exec_ctx,
        boolean takes_args, Object...to_exec_args)
        throws ApplicationException, BackoutException, NetworkException
    {
        endpt_to_run_on.handle_rpc_call(
            to_exec_method_name,exec_ctx,to_exec_args);
    }
			
    public void run()
        throws ApplicationException, BackoutException, NetworkException
    {
        static_run(
            endpt_to_run_on,to_exec_method_name,exec_ctx,
            takes_args,to_exec_args);
    }
}
