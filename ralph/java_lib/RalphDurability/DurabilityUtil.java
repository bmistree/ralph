package RalphDurability;

import java.util.List;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;
import ralph_protobuffs.UtilProto.UUID;

public class DurabilityUtil
{
    public static Durability prepare_proto_buf (
        String event_uuid, List<PartnerRequestSequenceBlock> rpc_args)
    {
        UUID.Builder uuid_builder = UUID.newBuilder();
        uuid_builder.setData(event_uuid);
        
        DurabilityPrepare.Builder prepare_msg = DurabilityPrepare.newBuilder();
        prepare_msg.setEventUuid(uuid_builder);
        for (PartnerRequestSequenceBlock req : rpc_args)
            prepare_msg.addRpcArgs(req);
        
        Durability.Builder to_return = Durability.newBuilder();
        to_return.setPrepare(prepare_msg);
        return to_return.build();
    }

    public static Durability complete_proto_buf (
        String event_uuid, boolean succeeded)
    {
        UUID.Builder uuid_builder = UUID.newBuilder();
        uuid_builder.setData(event_uuid);

        DurabilityComplete.Builder complete_msg =
            DurabilityComplete.newBuilder();
        complete_msg.setEventUuid(uuid_builder);
        complete_msg.setSucceeded(succeeded);
        
        Durability.Builder to_return = Durability.newBuilder();
        to_return.setComplete(complete_msg);
        return to_return.build();
    }
}