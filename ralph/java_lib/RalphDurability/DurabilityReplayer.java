package RalphDurability;

import java.util.Map;
import java.util.HashMap;

import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;
import ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.EndpointUUIDConstructorNamePair;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.PairedPartnerRequestSequenceEndpointUUID;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

import ralph.Connection.SingleSideConnection;

import ralph.Endpoint;
import ralph.EndpointConstructorObj;
import ralph.Util;
import ralph.InternalServiceFactory;
import ralph.RalphGlobals;
import ralph.IEndpointMap;
import ralph.RootEventParent;
import ralph.ActiveEvent;
import ralph.NonAtomicActiveEvent;
import ralph.MessageSender.IMessageSender;
import ralph.MessageSender.DurabilityReplayMessageSender;
import ralph.ExecutionContext.ExecutionContext;
import ralph.ExecutionContext.ReplayNonAtomicExecutionContext;


public class DurabilityReplayer implements IDurabilityReplayer, IEndpointMap
{
    private final ISerializedDurabilityReader durability_reader;
    private final Map<String, EndpointConstructorObj> constructor_map =
        new HashMap<String,EndpointConstructorObj>();
    private final Map<String, Endpoint> endpt_map =
        new HashMap<String,Endpoint>();
    
    public DurabilityReplayer(ISerializedDurabilityReader durability_reader)
    {
        this.durability_reader = durability_reader;
    }

    /**
       @param constructor_name --- The canonical name of the endpoint
       constructor object that's being used.
       
       @return --- Could be null if constructor name doesn't exist.
     */
    public synchronized EndpointConstructorObj get_constructor_obj(
        String constructor_name)
    {
        return constructor_map.get(constructor_name);
    }

    /**
       Replay prepare message.
     */
    private synchronized void handle_prepare_completed(
        DurabilityPrepare prepare_msg, RalphGlobals ralph_globals)
    {
        DurabilityReplayContext durability_replay_context =
            new DurabilityReplayContext(prepare_msg,this);
        // means that the event just created an endpoint, which is
        // externally reachable.
        if (prepare_msg.getRpcArgsCount() == 0)
        {
            for (EndpointUUIDConstructorNamePair pair:
                     prepare_msg.getEndpointsCreatedList())
            {
                String constructor_name =
                    pair.getConstructorCanonicalName();

                EndpointConstructorObj constructor =
                    constructor_map.get(constructor_name);
                //// DEBUG
                if (constructor == null)
                {
                    Util.logger_assert(
                        "Unknown endpt constructor during replay");
                }
                //// END DEBUG

                // FIXME: only allowing rebuilding endpoints as
                // SingleSideConnections and with no new
                // DurabilityContext-s
                Endpoint endpt = constructor.construct(
                    ralph_globals, null, durability_replay_context);
                add_endpoint(endpt);
            }
        }
        else
        {
            int next_index =
                durability_replay_context.get_next_index_and_increment();
            while (next_index != -1)
            {
                // get entry rpc message.
                PairedPartnerRequestSequenceEndpointUUID entry_call = 
                    prepare_msg.getRpcArgs(next_index);
                String evt_uuid = prepare_msg.getEventUuid().getData();
                Endpoint to_run_on =
                    DurabilityReplayContext.get_endpt_associated_with_paired_rpc(
                        entry_call,this);
                String method_to_run =
                    DurabilityReplayContext.get_method_call_associated_with_paired_rpc(
                        entry_call);
                PartnerRequestSequenceBlock req_seq_block =
                    entry_call.getRpcArgs();

                DurabilityReplayMessageSender msg_sender =
                    new DurabilityReplayMessageSender(
                        durability_replay_context);

                ReplayNonAtomicExecutionContext exec_ctx =
                    to_run_on.exec_ctx_map.replay_create_root_non_atomic_exec_ctx(
                        to_run_on, method_to_run, msg_sender,this);
                durability_replay_context.durable_replay_exec_rpc(
                    exec_ctx, to_run_on, req_seq_block);

                next_index =
                    durability_replay_context.get_next_index_and_increment();
            }
        }
    }

    @Override
    public synchronized boolean step(RalphGlobals ralph_globals)
    {
        // for now, just loading with endpoint constructor objects.
        DurabilityEvent event = durability_reader.next_durability_event();
        if (event == null)
            return false;

        if (event.event_type ==
            DurabilityEvent.DurabilityEventType.SERVICE_FACTORY)
        {
            ServiceFactoryDelta delta = event.service_factory_msg;
            EndpointConstructorObj constructor = 
                InternalServiceFactory.deserialize_endpt_constructor(
                    delta.getSerializedFactory());
            
            String constructor_name = constructor.get_canonical_name();
            constructor_map.put(constructor_name,constructor);
        }
        else if (event.event_type ==
                 DurabilityEvent.DurabilityEventType.COMPLETED)
        {
            handle_prepare_completed(event.prepare_msg, ralph_globals);
        }
        else if (event.event_type ==
                 DurabilityEvent.DurabilityEventType.OUTSTANDING)
        {
            Util.logger_warn(
                "Must handle outstanding durability messages when stepping");
        }
        //// DEBUG
        else
        {
            Util.logger_assert(
                "Unknown durability message when stepping");
        }
        //// END DEBUG
        return true;
    }
    
    @Override
    public synchronized long last_committed_local_lamport_timestamp()
    {
        Util.logger_assert(
            "Unimplemented last_committed_local_lamport_timestamp " +
            "in DurabilityReplayer");
        return -1;
    }

    @Override
    public synchronized void add_endpoint(Endpoint endpoint)
    {
        endpt_map.put(endpoint._uuid, endpoint);
    }
    
    @Override
    public synchronized void remove_endpoint_if_exists(Endpoint endpoint)
    {
        endpt_map.remove(endpoint._uuid);
    }
    
    /**
       @returns null if no endpoint available.
     */
    @Override
    public synchronized Endpoint get_endpoint_if_exists(String uuid)
    {
        return endpt_map.get(uuid);
    }
    
}