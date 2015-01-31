package RalphDurability;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import com.google.protobuf.ByteString;

import ralph.EndpointConstructorObj;
import ralph.Util;
import ralph.InternalServiceFactory;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;
import ralph_protobuffs.UtilProto.UUID;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.PairedPartnerRequestSequenceEndpointUUID;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.EndpointUUIDConstructorNamePair;
import ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta;

public class DurabilityContext implements IDurabilityContext
{
    public final String event_uuid;
    private final List<PartnerRequestSequenceBlock> rpc_args;
    // need to know which endpoint handled the rpc request.
    private final List<String> endpoint_uuid_received_rpc_on;
    private final List<EndptUUIDConstructorPair> endpts_created_info;
    
    // should set to true after we write a prepare message.  This way
    // we can skip writing complete messages for contexts that never
    // got to prepare step.
    private boolean has_prepared = false;
    
    public DurabilityContext(String event_uuid)
    {
        this.event_uuid = event_uuid;
        this.rpc_args = new ArrayList<PartnerRequestSequenceBlock>();
        this.endpoint_uuid_received_rpc_on = new ArrayList<String>();
        this.endpts_created_info = new ArrayList<EndptUUIDConstructorPair>();
    }


    @Override
    public synchronized DurabilityContext clone(String new_event_uuid)
    {
        DurabilityContext to_return = new DurabilityContext(new_event_uuid);

        for (int i = 0; i < rpc_args.size(); ++i)
        {
            PartnerRequestSequenceBlock rpc_arg = rpc_args.get(i);
            String endpoint_uuid = endpoint_uuid_received_rpc_on.get(i);
            to_return.add_rpc_arg(rpc_arg,endpoint_uuid);
        }

        for (EndptUUIDConstructorPair pair : endpts_created_info)
        {
            to_return.add_endpt_created_info(
                pair.endpt_uuid,pair.constructor_name);
        }
        return to_return;
    }

    @Override
    public synchronized void add_endpt_created_info(
        String endpt_uuid,String endpt_constructor_name)
    {
        endpts_created_info.add(
            new EndptUUIDConstructorPair(
                endpt_uuid,endpt_constructor_name));
    }

    private class EndptUUIDConstructorPair
    {
        public final String endpt_uuid;
        public final String constructor_name;
        public EndptUUIDConstructorPair(
            String endpt_uuid, String constructor_name)
        {
            this.endpt_uuid = endpt_uuid;
            this.constructor_name = constructor_name;
        }

        public EndpointUUIDConstructorNamePair to_proto_buf()
        {
            UUID.Builder uuid_builder = UUID.newBuilder();
            uuid_builder.setData(endpt_uuid);
            
            EndpointUUIDConstructorNamePair.Builder to_return =
                EndpointUUIDConstructorNamePair.newBuilder();
            to_return.setEndptUuid(uuid_builder);
            to_return.setConstructorCanonicalName(constructor_name);
            return to_return.build();
        }
    }
    
    @Override
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
        
        for (EndptUUIDConstructorPair pair : endpts_created_info)
            prepare_msg.addEndpointsCreated(pair.to_proto_buf());
        
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

    public static Durability endpt_constructor_durability_constructor(
        EndpointConstructorObj endpt_constructor_obj)
    {
        ServiceFactoryDelta.Builder sfd_builder =
            ServiceFactoryDelta.newBuilder();

        try
        {
            ByteString contents =
                InternalServiceFactory.static_convert_constructor_to_byte_string(
                    endpt_constructor_obj);
            sfd_builder.setSerializedFactory(contents);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Exception converting endpt to bytestring");
        }

        Durability.Builder to_return = Durability.newBuilder();
        to_return.setServiceFactory(sfd_builder);
        return to_return.build();
    }
}