package RalphDurability;

import java.util.List;
import java.util.ArrayList;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;
import ralph_protobuffs.UtilProto.UUID;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.PairedPartnerRequestSequenceEndpointUUID;

public class DurabilityContext
{
    public final String event_uuid;
    private final List<PartnerRequestSequenceBlock> rpc_args;
    // need to know which endpoint handled the rpc request.
    private final List<String> endpoint_uuid_received_rpc_on;

    // should set to true after we write a prepare message.  This way
    // we can skip writing complete messages for contexts that never
    // got to prepare step.
    private boolean has_prepared = false;
    
    public DurabilityContext(String event_uuid)
    {
        this.event_uuid = event_uuid;
        this.rpc_args = new ArrayList<PartnerRequestSequenceBlock>();
        this.endpoint_uuid_received_rpc_on = new ArrayList<String>();
    }

    /**
       Makes a deep copy of the state of the current durability
       context with a different event_uuid (new_event_uuid).  Basic
       idea is that from a non-atomic event, when we encounter an
       atomic event, we want to copy its durability context.  We only
       do stable logging when we complete atomic events.
     */
    public synchronized DurabilityContext clone(String new_event_uuid)
    {
        DurabilityContext to_return = new DurabilityContext(new_event_uuid);

        for (int i = 0; i < rpc_args.size(); ++i)
        {
            PartnerRequestSequenceBlock rpc_arg = rpc_args.get(i);
            String endpoint_uuid = endpoint_uuid_received_rpc_on.get(i);
            to_return.add_rpc_arg(rpc_arg,endpoint_uuid);
        }
        return to_return;
    }
    
    public synchronized void add_rpc_arg(
        PartnerRequestSequenceBlock arg, String endpoint_uuid)
    {
        rpc_args.add(arg);
        endpoint_uuid_received_rpc_on.add(endpoint_uuid);
    }

    public synchronized Durability prepare_proto_buf()
    {
        UUID.Builder uuid_builder = UUID.newBuilder();
        uuid_builder.setData(event_uuid);
        
        DurabilityPrepare.Builder prepare_msg = DurabilityPrepare.newBuilder();
        prepare_msg.setEventUuid(uuid_builder);

        for (int i = 0; i < rpc_args.size(); ++i)
        {
            PartnerRequestSequenceBlock prsb = rpc_args.get(i);
            String endpt_uuid = endpoint_uuid_received_rpc_on.get(i);
            UUID.Builder endpt_uuid_builder = UUID.newBuilder();
            endpt_uuid_builder.setData(endpt_uuid);
                        
            PairedPartnerRequestSequenceEndpointUUID.Builder rpc_arg_tuple =
                PairedPartnerRequestSequenceEndpointUUID.newBuilder();
            rpc_arg_tuple.setRpcArgs(prsb);
            rpc_arg_tuple.setEndpointUuid(endpt_uuid_builder);

            prepare_msg.addRpcArgs(rpc_arg_tuple);
        }
        
        Durability.Builder to_return = Durability.newBuilder();
        to_return.setPrepare(prepare_msg);
        has_prepared = true;
        return to_return.build();
    }


    public synchronized Durability complete_proto_buf(boolean succeeded)
    {
        if (! has_prepared)
            return null;
        
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