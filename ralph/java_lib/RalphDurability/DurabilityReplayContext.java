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
import ralph.IEndpointMap;
import ralph.Util;

public class DurabilityReplayContext implements IDurabilityReplayContext
{
    private final List<Endpoint> endpts_created_during_event =
        new ArrayList<Endpoint>();
    
    private final DurabilityPrepare prepare_msg;
    private int next_endpt_uuid_index = 0;
    
    // the index to use for to read into rpc vector.  not separating
    // rpc requests from responses yet because we wouldn't be able to
    // distinguish if A issues an RPC to B and then B issues an RPC to
    // A before returning from A issues an RPC to B, B returns, and
    // then B issues an RPC to A.  Index starts at 1 because first rpc
    // is entry point.
    private int next_rpc_request_index = 1;

    private final IEndpointMap endpt_map;
    
    public DurabilityReplayContext(
        DurabilityPrepare prepare_msg, IEndpointMap endpt_map)
    {
        this.prepare_msg = prepare_msg;
        this.endpt_map = endpt_map;
    }

    
    /**
       @return true if sequence_block is an rpc result instead of
       request.
     */
    private boolean is_result_sequence_block(
        PartnerRequestSequenceBlock sequence_block)
    {
        if (sequence_block.getNameOfBlockRequesting().equals(""))
            return true;
        return false;
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
    public RalphObject issue_rpc(
        RalphGlobals ralph_globals, ActiveEvent active_event)
    {
        PairedPartnerRequestSequenceEndpointUUID pair = 
            prepare_msg.getRpcArgs(next_rpc_request_index);
        ++ next_rpc_request_index;
        PartnerRequestSequenceBlock req_seq_block = pair.getRpcArgs();

        // if it's an rpc result, then we should return the result
        // immediately.  if it's not an rpc result, it means that
        // there may have been nested rpcs, and we need to process the
        // nested rpc calls in the meantime.
        if (is_result_sequence_block(req_seq_block))
        {
            if (! req_seq_block.hasReturnObjs())
                return null;

            Arguments return_objs = req_seq_block.getReturnObjs();
            // if this was the response to an rpc that returned a value,
            // then return it here.
            return RPCDeserializationHelper.return_args_to_ralph_object(
                return_objs, ralph_globals, active_event);
        }

        // FIXME:
        Util.logger_assert("\nStill must handle case of nested RPCs.");
        return null;
    }
}