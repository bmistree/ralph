package RalphVersions;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;

import ralph.CommitMetadata;
import ralph.EndpointConstructorObj;
import ralph.EnumConstructorObj;

import ralph_version_protobuffs.DeltaProto.Delta;
import ralph_version_protobuffs.ObjectContentsProto.ObjectContents;

public class InMemoryLocalVersionManager
    implements ILocalVersionSaver, ILocalVersionReplayer
{
    // key is the event_uuid held by CommitMetadata
    private final Map<String,CommitMetadata> commit_metadata_map =
        new HashMap<String,CommitMetadata>();

    // key is object uuid
    private final Map<String,ObjectHistory> object_history_map =
        new HashMap<String,ObjectHistory>();

    // key is the classname of the endpoint constructor object.
    private final Map<String,EndpointConstructorObj> endpoint_constructor_map =
        new HashMap<String,EndpointConstructorObj>();

    // key is the classname of the enum constructor object.
    private final Map<String,EnumConstructorObj> enum_constructor_map =
        new HashMap<String,EnumConstructorObj>();

    // key is the endpoint uuid
    private final Map<String,EndpointInitializationHistory> endpoint_initialization_map =
        new HashMap<String,EndpointInitializationHistory>();

    /**
       @returns null if does not exist.
    */
    @Override
    synchronized public EndpointConstructorObj get_endpoint_constructor_obj(
        String endpoint_constructor_obj_classname)
    {
        return endpoint_constructor_map.get(
            endpoint_constructor_obj_classname);
    }

    /**
       @returns null if does not exist.
    */
    @Override
    synchronized public EnumConstructorObj get_enum_constructor_obj(
        String enum_constructor_obj_classname)
    {
        return enum_constructor_map.get(enum_constructor_obj_classname);
    }

    @Override
    public void flush()
    {
        // nothing to flush
    }
    
    /**
       @returns null if does not exist.
     */
    @Override
    synchronized public ObjectHistory get_full_object_history(String obj_uuid)
    {
        return object_history_map.get(obj_uuid);
    }

    @Override
    synchronized public ObjectHistory get_ranged_object_history(
        String obj_uuid,Long lower_range, Long upper_range)
    {
        if (!object_history_map.containsKey(obj_uuid))
            return null;

        ObjectHistory obj_history = object_history_map.get(obj_uuid);
        return obj_history.produce_range(lower_range,upper_range);
    }
    
    /**
       @returns null if does not exist.
     */
    @Override
    synchronized public EndpointInitializationHistory
        get_endpoint_initialization_history(String endpoint_uuid)
    {
        return endpoint_initialization_map.get(endpoint_uuid);
    }
    
    @Override
    synchronized public void save_commit_metadata(
        CommitMetadata commit_metadata)
    {
        commit_metadata_map.put(commit_metadata.event_uuid,commit_metadata);
    }

    /**
       @returns null, if does not exist
     */
    synchronized public CommitMetadata get_commit_metadata(String event_uuid)
    {
        return commit_metadata_map.get(event_uuid);
    }
    
    @Override
    synchronized public void save_version_data(
        String object_uuid, Delta delta, CommitMetadata commit_metadata)
    {
        check_and_insert_in_memory_object_history(object_uuid);
        ObjectHistory object_history =
            object_history_map.get(object_uuid);

        object_history.add_delta(
            commit_metadata.root_commit_lamport_time,delta,
            commit_metadata.event_uuid);
    }

    @Override
    synchronized public void save_object_constructor(
        String object_uuid, ObjectContents obj_contents)
    {
        check_and_insert_in_memory_object_history(object_uuid);
        ObjectHistory object_history =
            object_history_map.get(object_uuid);
        object_history.set_construction_contents(obj_contents);
    }

    /**
       Insert an entry for object history in object_history_map.
     */
    synchronized private void check_and_insert_in_memory_object_history(
        String object_uuid)
    {
        if (! object_history_map.containsKey(object_uuid))
        {
            object_history_map.put(
                object_uuid, new ObjectHistory(object_uuid));
        }
    }
    
    @Override
    synchronized public void save_endpoint_global_mapping(
        String variable_name, String object_uuid,String endpoint_uuid,
        String endpoint_constructor_class_name,long local_lamport_time)
    {
        if (! endpoint_initialization_map.containsKey(endpoint_uuid))
        {
            endpoint_initialization_map.put(
                endpoint_uuid,
                new EndpointInitializationHistory(
                    endpoint_uuid,endpoint_constructor_class_name,
                    local_lamport_time));
        }
        
        EndpointInitializationHistory endpoint_initialization_history =
            endpoint_initialization_map.get(endpoint_uuid);
        endpoint_initialization_history.add_variable(variable_name,object_uuid);
    }

    @Override
    synchronized public void save_endpoint_constructor_obj(
        EndpointConstructorObj endpoint_constructor_obj)
    {
        endpoint_constructor_map.put(
            endpoint_constructor_obj.getClass().getName(),
            endpoint_constructor_obj);
    }

    @Override
    synchronized public void save_enum_constructor_obj(
        EnumConstructorObj enum_constructor_obj)
    {
        enum_constructor_map.put(
            enum_constructor_obj.getClass().getName(),
            enum_constructor_obj);
    }
    
    @Override
    public void close_versioned_object(String object_uuid)
    {
        // Do nothing for now.
    }
}
