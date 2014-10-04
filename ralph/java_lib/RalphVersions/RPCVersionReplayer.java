package RalphVersions;

import java.util.HashMap;
import java.util.Map;

import ralph.EndpointConstructorObj;
import ralph.EnumConstructorObj;
import ralph.Util;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.ArgumentContainerDeltas;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.DeltaProto.Delta;

public class RPCVersionReplayer implements IVersionReplayer
{
    private final Map<String,ObjectHistory> object_history_map =
        new HashMap<String,ObjectHistory>();
    private final Map<String,ObjectContents> object_contents_map =
        new HashMap<String,ObjectContents>();
    
    
    public RPCVersionReplayer(Arguments arguments)
    {
        for (ObjectContents obj_contents : arguments.getContextContentsList())
        {
            String obj_uuid = obj_contents.getUuid();
            ObjectHistory obj_history = new ObjectHistory(obj_uuid);
            obj_history.set_construction_contents(obj_contents);
            object_history_map.put(obj_uuid,obj_history);
            object_contents_map.put(obj_uuid,obj_contents);
        }

        for (ArgumentContainerDeltas arg_container_delta :
                 arguments.getContainerDeltasList())
        {
            // FIXME: only wrapping container delta here in
            // Delta.Builder because SingleObjectChange requires
            // taking in a full Delta.  All that's going to happen
            // during reconstruction, however, is that we're going to
            // re-unwrap it.  Could likely save the 
            Delta.Builder wrapper_delta = Delta.newBuilder();
            for (Delta.ContainerDelta container_delta :
                     arg_container_delta.getContainerDeltaList())
            {
                wrapper_delta.addContainerDelta(container_delta);
            }
            
            // insert wrappted delta into object history.
            String obj_uuid = arg_container_delta.getObjectUuid();
            //// DEBUG
            if (! object_history_map.containsKey(obj_uuid))
            {
                Util.logger_assert(
                    "Received delta for unknown serialized object.");
            }
            //// END DEBUG
            
            
            ObjectHistory obj_history = object_history_map.get(obj_uuid);
            // For rpc variables, actual timestamp and
            // commit_metadata_event_uuid are unimportant.  This is
            // because we are replaying fully.  Regardless of
            // timestamp, will go through full history.
            obj_history.add_delta(0L,wrapper_delta.build(),null);
        }
    }

    public ObjectContents get_object_contents(String obj_uuid)
    {
        return object_contents_map.get(obj_uuid);
    }
    
    /**
       @returns null if does not exist.
     */
    public ObjectHistory get_full_object_history(String obj_uuid)
    {
        return object_history_map.get(obj_uuid);
    }

    /**
       @returns null if does not exist.
     */
    public EnumConstructorObj get_enum_constructor_obj(
        String enum_constructor_obj_classname)
    {
        Util.logger_assert("Still must enable serializing enums in rpcs.");
        return null;
    }
    
    public ObjectHistory get_ranged_object_history(
        String obj_uuid,Long lower_range, Long upper_range)
    {
        // reason that should never request a range for an rpc object
        // is that we should only ever be generating a full history
        // for objects.  And timestamps entered when generating
        // ObjectHistory-s in constructor do not actually map to real
        // Lamport timestamps.
        Util.logger_assert(
            "Should never request a ranged object history for an " +
            "RPC argument.");
        return null;
    }

    /**
       Do not permit directly serializing and sending endpoint
       references across network.
     */
    public EndpointConstructorObj get_endpoint_constructor_obj(
        String endpoint_constructor_obj_classname)
    {
        Util.logger_assert(
            "Do not support transmitting endpoints across rpcs.");
        return null;
    }
    public EndpointInitializationHistory
        get_endpoint_initialization_history(String endpoint_uuid)
    {
        Util.logger_assert(
            "Do not support transmitting endpoints across rpcs.");
        return null;
    }

}