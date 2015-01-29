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

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

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
        endpt_map.add_endpoint(new_endpt);
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

    public static Endpoint get_endpt_associated_with_paired_rpc(
        PairedPartnerRequestSequenceEndpointUUID pair, IEndpointMap endpt_map)
    {
        String endpt_uuid = pair.getEndpointUuid().getData();
        return endpt_map.get_endpoint_if_exists(endpt_uuid);
    }

    
    @Override
    public RalphObject issue_rpc(
        RalphGlobals ralph_globals, ActiveEvent active_event)
    {
        while (true)
        {
            PairedPartnerRequestSequenceEndpointUUID pair = 
                prepare_msg.getRpcArgs(next_rpc_request_index);
            ++ next_rpc_request_index;
            //// DEBUG
            if (pair == null)
            {
                Util.logger_assert(
                    "Ran out of RPCs to call.  Maybe number of " +
                    "results do not match number of requests?");
            }
            //// END DEBUG

            
            PartnerRequestSequenceBlock req_seq_block = pair.getRpcArgs();

            // CASE 1: Next operation is an rpc result.  We should
            // return it immediately.
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

            // CASE 2: Next operation is not an rpc result, but
            // another RPC request.  This could happen, for instance,
            // if the remote host we issued an RPC to issued an RPC
            // back to this host before returning.  In this case, we
            // must process the RPC call before continuing.
            Endpoint to_rpc_on =
                get_endpt_associated_with_paired_rpc(pair,endpt_map);
            
            //// DEBUG
            if (to_rpc_on == null)
            {
                Util.logger_assert(
                    "Requested to issue RPC to an unknown endpt " +
                    "during durability replay.");
            }
            //// END DEBUG

            try
            {
                active_event.recv_partner_sequence_call_msg(
                    to_rpc_on, req_seq_block);
            }
            catch(ApplicationException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Unexpected application exception in replay context.");
            }
            catch (BackoutException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Unexpected backout exception in replay context.");
            }
            catch(NetworkException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Unexpected network exception in replay context.");
            }
        }
    }
}