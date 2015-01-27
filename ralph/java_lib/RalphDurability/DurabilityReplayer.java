package RalphDurability;

import java.util.Map;
import java.util.HashMap;

import ralph.Endpoint;
import ralph.EndpointConstructorObj;
import ralph.Util;
import ralph.InternalServiceFactory;
import ralph.RalphGlobals;

import RalphConnObj.SingleSideConnection;

import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;
import ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare.EndpointUUIDConstructorNamePair;


public class DurabilityReplayer implements IDurabilityReplayer
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
            new DurabilityReplayContext(prepare_msg);
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
                    ralph_globals,new SingleSideConnection(),
                    null,durability_replay_context);
                endpt_map.put(endpt.uuid(),endpt);
            }
        }
        else
        {
            Util.logger_warn(
                "Must handle prepare message in durability replayer");
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
    public synchronized Endpoint get_endpt(String endpt_uuid)
    {
        return endpt_map.get(endpt_uuid);
    }

    @Override
    public synchronized long last_committed_local_lamport_timestamp()
    {
        Util.logger_assert(
            "Unimplemented last_committed_local_lamport_timestamp " +
            "in DurabilityReplayer");
        return -1;
    }
}