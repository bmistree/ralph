package RalphCallResults;

import ralph_protobuffs.VariablesProto.Variables;

public class MessageCallResultObject
{
    public static enum ResultType
    {
        APPLICATION_EXCEPTION, BACKOUT_BEFORE_RECEIVE_MESSAGE,
        COMPLETED, NETWORK_FAILURE, STOP_ALREADY_CALLED
    }

    /**
       Common to all.
     */
    public ResultType result_type;

    /**
       Used for exceptions.
     */
    public String trace;
    
    /**
       Used for completeds.  @see comment above
       self.message_listening_queues_map in
       ralphActiveEvent._ActiveEvent.
    */
    public String reply_with_msg_field;
	
    /**
     *
     Used for completeds.  In a message sequence, tells us the next
     internal function to execute in the sequence.  If it is None,
     then it means that there is no more to execute in the sequence.
    */
    public String to_exec_next_name_msg_field = null;
	

    /**
     *
     Used for completeds.  Should be able to put this map directly
     into a _VariableStore object to update each of an event's pieces
     of peered data.  @see
     VariableStore._VariableStore.incorporate_deltas
    */
    public Variables returned_variables = null;

    /**
       If the rpc was supposed to return a value, that value will be
       in returned_objs.
     */
    public Variables returned_objs = null;
    
    /**
       Only have private constructor.  Otherwise, should use one of
       static methods to produce a new object.
     */
    private MessageCallResultObject(ResultType rtype)
    {
        result_type = rtype;
    }

    public static MessageCallResultObject application_exception(String _trace)
    {
        MessageCallResultObject to_return =
            new MessageCallResultObject(ResultType.APPLICATION_EXCEPTION);
        to_return.trace = _trace;
        return to_return;
    }

    public static MessageCallResultObject backout_before_receive_message()
    {
        MessageCallResultObject to_return =
            new MessageCallResultObject(
                ResultType.BACKOUT_BEFORE_RECEIVE_MESSAGE);
        return to_return;
    }

    public static MessageCallResultObject completed(
        String _reply_with_msg_field, String _to_exec_next_name_msg_field,
        Variables _returned_variables,Variables _returned_objs)
    {
        MessageCallResultObject to_return =
            new MessageCallResultObject(ResultType.COMPLETED);

        to_return.reply_with_msg_field = _reply_with_msg_field;
        to_return.to_exec_next_name_msg_field = _to_exec_next_name_msg_field;
        to_return.returned_variables = _returned_variables;
        to_return.returned_objs = _returned_objs;
        return to_return;
    }

    public static MessageCallResultObject network_failure(String _trace)
    {
        MessageCallResultObject to_return =
            new MessageCallResultObject(ResultType.NETWORK_FAILURE);
        to_return.trace = _trace;
        return to_return;
    }

    public static MessageCallResultObject stop_already_called()
    {
        MessageCallResultObject to_return = 
            new MessageCallResultObject(ResultType.STOP_ALREADY_CALLED);
        return to_return;
    }
}
