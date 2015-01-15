package ralph;

import java.util.List;


import RalphExceptions.BackoutException;

import ralph_protobuffs.UtilProto;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;


public class PartnerRequestSequenceBlockProducer
{
    public static PartnerRequestSequenceBlock produce_request_block(
        ExecutingEventContext ctx,String func_name, boolean first_msg,
        List<RalphObject> args,RalphObject result, 
        ActiveEvent active_event, boolean atomic,String reply_with_uuid)
        throws BackoutException
    {
        SerializationContext serialization_context =
            new SerializationContext(args,true);
        Arguments.Builder serialized_arguments =
            serialization_context.serialize_all(active_event);

        Arguments.Builder serialized_results = null;
        if (result != null)
        {
            serialization_context = new SerializationContext(result,true);
            serialized_results =
                serialization_context.serialize_all(active_event);
        }
        
        // changed to have rpc semantics: this means that if it's not
        // the first message, then it is a reply to another message.
        // if it is a first message, then should not be replying to
        // anything.
        String replying_to = null;
        if (! first_msg)
            replying_to = ctx.get_to_reply_with();


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
    	if (replying_to != null)
    	{
            UtilProto.UUID.Builder reply_to_uuid_msg =
                UtilProto.UUID.newBuilder();
            reply_to_uuid_msg.setData(replying_to);
            request_sequence_block_msg.setReplyToUuid(reply_to_uuid_msg);
    	}

        request_sequence_block_msg.setArguments(serialized_arguments);
        if (serialized_results != null)
            request_sequence_block_msg.setReturnObjs(serialized_results);

        return request_sequence_block_msg.build();
    }
}