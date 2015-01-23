package RalphDurability;

import java.util.Map;
import java.util.HashMap;

import ralph.Endpoint;
import ralph.EndpointConstructorObj;
import ralph.Util;
import ralph.InternalServiceFactory;

import ralph_protobuffs.DurabilityProto.Durability;
import ralph_protobuffs.DurabilityPrepareProto.DurabilityPrepare;
import ralph_protobuffs.DurabilityCompleteProto.DurabilityComplete;
import ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta;

public class DurabilityReplayer implements IDurabilityReplayer
{
    private final IDurabilityReader durability_reader;
    private final Map<String, EndpointConstructorObj> constructor_map =
        new HashMap<String,EndpointConstructorObj>();
    
    
    public DurabilityReplayer(IDurabilityReader durability_reader)
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
    
    @Override
    public synchronized boolean step()
    {
        // for now, just loading with endpoint constructor objects.
        Durability msg = durability_reader.get_durability_msg();
        if (msg == null)
            return false;

        if (msg.hasServiceFactory())
        {
            ServiceFactoryDelta delta = msg.getServiceFactory();
            EndpointConstructorObj constructor = 
                InternalServiceFactory.deserialize_endpt_constructor(
                    delta.getSerializedFactory());
            
            String constructor_name = constructor.get_canonical_name();
            constructor_map.put(constructor_name,constructor);
        }
        else if (msg.hasPrepare())
        {
            Util.logger_warn(
                "Must handle prepare message in durability replayer");
        }
        else if (msg.hasComplete())
        {
            Util.logger_warn(
                "Must handle complete message in durability replayer");
        }
        else
        {
            Util.logger_assert(
                "Unknown durability message when stepping");
        }
        return true;
    }
    

    @Override
    public synchronized Endpoint get_endpt(String endpt_uuid)
    {
        Util.logger_assert(
            "Unimplemented get_endpt method in DurabilityReplayer");
        return null;
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