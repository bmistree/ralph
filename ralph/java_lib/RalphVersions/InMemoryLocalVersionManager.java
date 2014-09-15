package RalphVersions;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;

import ralph.CommitMetadata;
import ralph.EndpointConstructorObj;

import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

public class InMemoryLocalVersionManager implements ILocalVersionManager
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

    // key is the endpoint uuid
    private final Map<String,InMemoryEndpointInitializationHistory> endpoint_initialization_map =
        new HashMap<String,InMemoryEndpointInitializationHistory>();

    /**
       @returns null if does not exist.
     */
    synchronized public ObjectHistory get_object_history(String obj_uuid)
    {
        return object_history_map.get(obj_uuid);
    }
    
    @Override
    synchronized public void save_commit_metadata(CommitMetadata commit_metadata)
    {
        commit_metadata_map.put(commit_metadata.event_uuid,commit_metadata);
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
                new InMemoryEndpointInitializationHistory(
                    endpoint_uuid,endpoint_constructor_class_name,
                    local_lamport_time));
        }
        
        InMemoryEndpointInitializationHistory endpoint_initialization_history =
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
    public void close_versioned_object(String object_uuid)
    {
        // Do nothing for now.
    }
    
    private class InMemoryEndpointInitializationHistory
    {
        public final String endpoint_uuid;
        public final String endpoint_constructor_class_name;
        public final long local_lamport_time;
        // tuple is name of variable and uuid of object (not
        // endpoint).
        public final List<NameUUIDTuple> variable_list =
            new ArrayList<NameUUIDTuple>();
        
        public InMemoryEndpointInitializationHistory(
            String endpoint_uuid, String endpoint_constructor_class_name,
            long local_lamport_time)
        {
            this.endpoint_uuid = endpoint_uuid;
            this.endpoint_constructor_class_name =
                endpoint_constructor_class_name;
            this.local_lamport_time = local_lamport_time;
        }

        public void add_variable(String name, String object_uuid)
        {
            variable_list.add(
                new NameUUIDTuple(name,object_uuid));
        }

        public class NameUUIDTuple
        {
            public final String name;
            public final String uuid;
            public NameUUIDTuple(String name, String uuid)
            {
                this.name = name;
                this.uuid = uuid;
            }
        }
    }
}
