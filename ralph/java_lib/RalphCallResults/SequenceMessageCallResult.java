package RalphCallResults;

import ralph_protobuffs.VariablesProto.Variables;

public class SequenceMessageCallResult extends MessageCallResultObject{

    /**
       @see comment above self.message_listening_queues_map in
       ralphActiveEvent._ActiveEvent.
    */
    public String reply_with_msg_field;
	
    /**
     * In a message sequence, tells us the next internal function to
     execute in the sequence.  If it is None, then it means that
     there is no more to execute in the sequence.
    */
    public String to_exec_next_name_msg_field = null;
	

    /**
     * 	Should be able to put this map directly into a _VariableStore object
     to update each of an event's pieces of peered data.  @see
     VariableStore._VariableStore.incorporate_deltas
    */
    public Variables returned_variables = null;
	
    // We must update the event context with the new
    // reply_with_msg_field when complete.

    public SequenceMessageCallResult(
        String _reply_with_msg_field, String _to_exec_next_name_msg_field,
        Variables _returned_variables)
    {
    	reply_with_msg_field = _reply_with_msg_field;
    	to_exec_next_name_msg_field = _to_exec_next_name_msg_field;
        returned_variables = _returned_variables;
    }
}
