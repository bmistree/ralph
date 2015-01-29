package RalphDurability;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Queue;

import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.EndpointUUIDConstructorNamePair;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.PairedPartnerRequestSequenceEndpointUUID;

import ralph.Endpoint;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.RPCDeserializationHelper;

public class DurabilityReplayContext implements IDurabilityReplayContext
{
    private final List<Endpoint> endpts_created_during_event =
        new ArrayList<Endpoint>();

    private final Queue<PartnerRequestSequenceBlock> rpc_result_queue =
        new ArrayDeque<PartnerRequestSequenceBlock>();
    
    private final DurabilityPrepare prepare_msg;
    private int next_endpt_uuid_index = 0;
    
    public DurabilityReplayContext(DurabilityPrepare prepare_msg)
    {
        this.prepare_msg = prepare_msg;
        
        for (PairedPartnerRequestSequenceEndpointUUID pair : 
                 prepare_msg.getRpcArgsList())
        {
            PartnerRequestSequenceBlock rpc_arg = pair.getRpcArgs();
            // result objects should have empty names of blocks to
            // request.
            if (rpc_arg.getNameOfBlockRequesting().equals(""))
                rpc_result_queue.add(rpc_arg);
        }
    }
    
    @Override
    public void register_endpoint(Endpoint new_endpt)
    {
        endpts_created_during_event.add(new_endpt);
    }
    
    /**
       Returns the next endpoint uuid an endpoint should use during
       construction.
     */
    @Override
    public String next_endpt_uuid()
    {
        EndpointUUIDConstructorNamePair pair =
            prepare_msg.getEndpointsCreated(next_endpt_uuid_index);
        ++next_endpt_uuid_index;

        return pair.getEndptUuid().getData();
    }

    /**
       Return a list of all endpoints that were created as part of
       replaying this event.
     */
    @Override
    public List<Endpoint> all_generated_endpoints()
    {
        return endpts_created_during_event;
    }

    @Override
    public RalphObject next_rpc_result(
        RalphGlobals ralph_globals, ActiveEvent active_event)
    {
        PartnerRequestSequenceBlock request_sequence_block =
            rpc_result_queue.remove();

        if (! request_sequence_block.hasReturnObjs())
            return null;

        Arguments return_objs = request_sequence_block.getReturnObjs();
        // if this was the response to an rpc that returned a value,
        // then return it here.
        return RPCDeserializationHelper.return_args_to_ralph_object(
            return_objs, ralph_globals, active_event);
    }
}