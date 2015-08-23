package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import ralph.RalphGlobals;
import ralph.EndpointConstructorObj;
import ralph.Endpoint;
import ralph.RalphObject;
import ralph.CommitMetadata;
import RalphVersions.EndpointInitializationHistory.NameUUIDTuple;
import ralph_protobuffs.DeltaProto.Delta;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;


import ralph_protobuffs.VersionSaverMessagesProto.VersionSaverMessages;

public class VersionUtil
{
    /**
       @param endpt_constructor_class_name_to_obj --- Keys are
       endpoint constructor object class names, values are
       EndpointConstructorObjs.

       @param to_rebuild_until --- If null, means should replay until
       run out of version history.
     */
    public static Endpoint rebuild_endpoint(
        String endpoint_uuid,
        RalphGlobals ralph_globals,
        IReconstructionContext reconstruction_context,
        Long to_rebuild_until)
    {
        IVersionReplayer version_replayer =
            reconstruction_context.get_version_replayer();

        EndpointInitializationHistory endpt_history =
            version_replayer.get_endpoint_initialization_history(
                endpoint_uuid);
        EndpointConstructorObj endpt_constructor_obj =
            version_replayer.get_endpoint_constructor_obj(
                endpt_history.endpoint_constructor_class_name);

        // repopulate all initial ralph objects that get placed in
        // endpoint.
        List<RalphObject> endpt_initialization_vars =
            new ArrayList<RalphObject>();

        for (NameUUIDTuple name_uuid_tuple : endpt_history.variable_list)
        {
            String obj_uuid = name_uuid_tuple.uuid;
            // putting null in for second parameter in order to play
            // all the way to end.
            RalphObject ralph_object =
                reconstruction_context.get_constructed_object(
                    obj_uuid,to_rebuild_until);
            endpt_initialization_vars.add(ralph_object);
        }

        return endpt_constructor_obj.construct(
            ralph_globals, endpt_initialization_vars, null);
    }


    public static Endpoint rebuild_endpoint(
        String endpoint_uuid,
        RalphGlobals ralph_globals,
        IReconstructionContext reconstruction_context)
    {
        return rebuild_endpoint(
            endpoint_uuid,ralph_globals,reconstruction_context,null);
    }

    public static VersionSaverMessages.CommitMetadata.Builder
        commit_metadata_message_builder(CommitMetadata commit_metadata)
    {
        VersionSaverMessages.CommitMetadata.Builder cm_builder =
            VersionSaverMessages.CommitMetadata.newBuilder();

        cm_builder.setRootCommitLamportTime(
            commit_metadata.root_commit_lamport_time);
        cm_builder.setRootApplicationUuid(
            commit_metadata.root_application_uuid);
        cm_builder.setEventName(commit_metadata.event_name);
        cm_builder.setEventUuid(commit_metadata.event_uuid);
        return cm_builder;
    }

    public static VersionSaverMessages.VersionData.Builder
        version_data_message_builder(
            String object_uuid, Delta delta,CommitMetadata commit_metadata)
    {
        VersionSaverMessages.VersionData.Builder version_data_builder =
            VersionSaverMessages.VersionData.newBuilder();
        version_data_builder.setObjectUuid(object_uuid);
        version_data_builder.setDelta(delta);
        version_data_builder.setCommitMetadataEventUuid(
            commit_metadata.event_uuid);
        return version_data_builder;
    }

    public static VersionSaverMessages.EndpointGlobalMapping.Builder
        endpoint_global_mapping_message_builder(
            String variable_name, String object_uuid,String endpoint_uuid,
            String endpoint_constructor_class_name,long local_lamport_time)
    {
        VersionSaverMessages.EndpointGlobalMapping.Builder endpoint_global_mapping_builder =
            VersionSaverMessages.EndpointGlobalMapping.newBuilder();

        endpoint_global_mapping_builder.setVariableName(variable_name);
        endpoint_global_mapping_builder.setObjectUuid(object_uuid);
        endpoint_global_mapping_builder.setEndpointUuid(endpoint_uuid);
        endpoint_global_mapping_builder.setEndpointConstructorClassName(
            endpoint_constructor_class_name);
        endpoint_global_mapping_builder.setLocalLamportTime(local_lamport_time);
        return endpoint_global_mapping_builder;
    }

    public static VersionSaverMessages.ObjectConstructor.Builder
        object_constructor_message_builder(
            String object_uuid, ObjectContents obj_contents)
    {
        VersionSaverMessages.ObjectConstructor.Builder object_constructor_builder =
            VersionSaverMessages.ObjectConstructor.newBuilder();
        object_constructor_builder.setObjectUuid(object_uuid);
        object_constructor_builder.setObjContents(obj_contents);
        return object_constructor_builder;
    }
}