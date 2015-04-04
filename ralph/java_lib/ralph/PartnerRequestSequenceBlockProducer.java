package ralph;

import java.util.List;


import RalphExceptions.BackoutException;

import ralph_protobuffs.UtilProto;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;


public class PartnerRequestSequenceBlockProducer
{

    /**
       @param {String or null} to_reply_to --- If we expect a response
       to this message, then tell the other side that it should
       respond with the uuid to_reply_to.
       
       @param {String or null} func_name --- The name of the sequence
       block we want to execute on the partner endpoint. (Note: this is
       how that sequence block is named in the source Waldo file, not
       how it is translated by the compiler into a function.)  It can
       also be None if this is the final message sequence block's
       execution.

       @param {List or null} args --- The positional arguments
       inserted into the call as an rpc.  Includes whether the
       argument is a reference or not (ie, we should update the
       variable's value on the caller).  Note that can be null if we
       have no args to pass back (or if is a sequence completed call).
       
       @param {uuid} reply_with_uuid --- When the partner endpoint
       responds, it should place reply_with_uuid in its reply_to message
       field.  That way, we can determine which message the partner
       endpoint was replying to.

       @param {ActiveEvent} active_event --- The active event that is
       requesting the message to be sent.

       @param {bool} first_msg --- If we are sending the first message
       in a sequence block, then we must force the sequence local data
       to be transmitted whether or not it was modified.

       @param target_endpt_uuid --- Null if reply.  Otherwise, should
       be the uuid of remote endpoint uuid.
     */
    public static PartnerRequestSequenceBlock produce_request_block(
        String to_reply_to, String func_name, List<? extends RalphObject> args,
        RalphObject result, ActiveEvent active_event, String reply_with_uuid,
        String target_endpt_uuid)
        throws BackoutException
    {
        // true if this call should be part of a transaction.  alse if
        // it's just a regular rpc.  Only keeps track if this is not
        // the first message sent.
        boolean atomic = active_event.rpc_should_be_atomic();
        
        Arguments.Builder serialized_arguments;
        if (args != null)
        {
            SerializationContext serialization_context =
                new SerializationContext(args,true);
            serialized_arguments =
                serialization_context.serialize_all(active_event);
        }
        else
            serialized_arguments = Arguments.newBuilder();

        Arguments.Builder serialized_results = null;
        if (result != null)
        {
            SerializationContext serialization_context =
                new SerializationContext(result,true);
            serialized_results =
                serialization_context.serialize_all(active_event);
        }

    	PartnerRequestSequenceBlock.Builder request_sequence_block_msg =
            PartnerRequestSequenceBlock.newBuilder();
    	request_sequence_block_msg.setTransaction(atomic);

    	// event uuid + priority
    	UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
    	event_uuid_msg.setData(active_event.uuid);
    	
    	UtilProto.Priority.Builder priority_msg =
            UtilProto.Priority.newBuilder();
    	priority_msg.setData(active_event.get_priority());
    	
    	request_sequence_block_msg.setEventUuid(event_uuid_msg);
    	request_sequence_block_msg.setPriority(priority_msg);
        
    	//name of block requesting
    	if (func_name != null)
            request_sequence_block_msg.setNameOfBlockRequesting(func_name);
    	
    	//reply with uuid
    	UtilProto.UUID.Builder reply_with_uuid_msg =
            UtilProto.UUID.newBuilder();
    	reply_with_uuid_msg.setData(reply_with_uuid);
    	
    	request_sequence_block_msg.setReplyWithUuid(reply_with_uuid_msg);
    	
    	//reply to uuid
    	if (to_reply_to != null)
    	{
            UtilProto.UUID.Builder reply_to_uuid_msg =
                UtilProto.UUID.newBuilder();
            reply_to_uuid_msg.setData(to_reply_to);
            request_sequence_block_msg.setReplyToUuid(reply_to_uuid_msg);
    	}

        request_sequence_block_msg.setArguments(serialized_arguments);
        if (serialized_results != null)
            request_sequence_block_msg.setReturnObjs(serialized_results);

        if (target_endpt_uuid != null)
        {
            UtilProto.UUID.Builder target_endpt_uuid_builder =
                UtilProto.UUID.newBuilder();
            target_endpt_uuid_builder.setData(target_endpt_uuid);
            
            request_sequence_block_msg.setTargetEndpt(
                target_endpt_uuid_builder);
        }
        
        return request_sequence_block_msg.build();
    }
}