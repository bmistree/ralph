package RalphCallResults;

import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas;

public class SequenceMessageCallResult extends MessageCallResultObject{

    /**
       @see comment above self.message_listening_queues_map in
       waldoActiveEvent._ActiveEvent.
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
     waldoVariableStore._VariableStore.incorporate_deltas
    */
    public VarStoreDeltas sequence_local_var_store_deltas = null;
	
    // We must update the event context with the new
    // reply_with_msg_field when complete.

    public SequenceMessageCallResult(
        String _reply_with_msg_field, String _to_exec_next_name_msg_field,
        VarStoreDeltas _sequence_local_var_store_deltas)
    {
    	reply_with_msg_field = _reply_with_msg_field;
    	to_exec_next_name_msg_field = _to_exec_next_name_msg_field;
    	sequence_local_var_store_deltas = _sequence_local_var_store_deltas;
    }
}
