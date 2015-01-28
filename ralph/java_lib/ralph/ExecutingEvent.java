package ralph;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import ralph.MessageSender.IMessageSender;

public class ExecutingEvent 
{
    private String to_exec_method_name;
    private ActiveEvent active_event;
    private IMessageSender message_sender;
    private Object[] to_exec_args;
    private boolean takes_args;
    private Endpoint endpt_to_run_on = null;
	
    /**
       @param {String} to_exec_method_name --- The name of the method
       to execute on this endpoint.

       @param {_ActiveEvent object} active_event --- The active event
       object that to_exec should use for accessing endpoint data.

       @param {IMessageSender} _message_sender ---
    
       @param {*args} to_exec_args ---- Any additional arguments that
       get passed to the closure to be executed.
    */
    public ExecutingEvent(
        Endpoint _endpt_to_run_on,
        String _to_exec_method_name,ActiveEvent _active_event,
        IMessageSender _message_sender,
        boolean _takes_args,// sequence calls do not take arguments
        Object..._to_exec_args)
    {
        endpt_to_run_on = _endpt_to_run_on;
        to_exec_method_name = _to_exec_method_name;
        active_event = _active_event;
        takes_args = _takes_args;
        message_sender = _message_sender;
        to_exec_args = _to_exec_args;
    }
	
    /**
     * @see arguments to constructor.
     */
    public static void static_run(
        Endpoint endpt_to_run_on,
        String to_exec_method_name,ActiveEvent active_event,
        IMessageSender message_sender,
        boolean takes_args, Object...to_exec_args)
        throws ApplicationException, BackoutException, NetworkException
    {
        endpt_to_run_on.handle_rpc_call(
            to_exec_method_name,active_event,
            message_sender,to_exec_args);
    }
			
    public void run()
        throws ApplicationException, BackoutException, NetworkException
    {
        static_run(
            endpt_to_run_on,to_exec_method_name,
            active_event,message_sender,takes_args,to_exec_args);
    }
}
