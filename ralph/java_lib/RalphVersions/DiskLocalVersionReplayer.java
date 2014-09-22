package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ralph.Util;
import ralph.EndpointConstructorObj;
import ralph.CommitMetadata;
import ralph.VersioningInfo;

import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.VersionSaverMessagesProto.VersionSaverMessages;


public class DiskLocalVersionReplayer implements ILocalVersionReplayer
{
    protected final String filename;
    protected boolean has_been_initialized = false;
    protected InMemoryLocalVersionManager local_version_manager = null;
    
    public DiskLocalVersionReplayer (String _filename)
    {
        filename = _filename;
    }


    /**
       Essentially, load all file data into local_version_manager, and
       use that to respond to queries.
     */
    protected void init_local_version_manager_if_unitialized()
    {
        if (local_version_manager != null)
            return;
        
        local_version_manager = new InMemoryLocalVersionManager();
        try
        {
            File file = new File(filename);
            FileInputStream file_input_stream = new FileInputStream(file);

            // have to buffer registering VersionData-s because need
            // to pass in associated CommitMetadata object.
            List<VersionSaverMessages.VersionData> buffered_version_datas =
                new ArrayList<VersionSaverMessages.VersionData>();
            while (file_input_stream.available() != 0)
            {
                VersionSaverMessages vsm = VersionSaverMessages.parseDelimitedFrom(
                    file_input_stream);
                if (vsm.hasVersionData())
                    buffered_version_datas.add(vsm.getVersionData());
                else if (vsm.hasCommitMetadata())
                {
                    VersionSaverMessages.CommitMetadata cm_msg =
                        vsm.getCommitMetadata();
                    CommitMetadata cm = new CommitMetadata(
                        cm_msg.getRootCommitLamportTime(),
                        cm_msg.getRootApplicationUuid(),
                        cm_msg.getEventName(),cm_msg.getEventUuid());
                    local_version_manager.save_commit_metadata(cm);
                }
                else if (vsm.hasEndpointGlobalMapping())
                {
                    VersionSaverMessages.EndpointGlobalMapping
                        endpt_global_mapping_msg =
                        vsm.getEndpointGlobalMapping();
                    local_version_manager.save_endpoint_global_mapping(
                        endpt_global_mapping_msg.getVariableName(),
                        endpt_global_mapping_msg.getObjectUuid(),
                        endpt_global_mapping_msg.getEndpointUuid(),
                        endpt_global_mapping_msg.getEndpointConstructorClassName(),
                        endpt_global_mapping_msg.getLocalLamportTime());
                }
                else if (vsm.hasObjectConstructor())
                {
                    VersionSaverMessages.ObjectConstructor obj_constructor_msg =
                        vsm.getObjectConstructor();
                    local_version_manager.save_object_constructor(
                        obj_constructor_msg.getObjectUuid(),
                        obj_constructor_msg.getObjContents());
                }
                //// DEBUG
                else
                {
                    Util.logger_assert(
                        "VersionSaverMessage without expected ");
                }
                //// END DEBUG
            }


            // now replay version data messages
            for (VersionSaverMessages.VersionData version_data : 
                     buffered_version_datas)
            {
                String commit_metadata_event_uuid =
                    version_data.getCommitMetadataEventUuid();
                CommitMetadata cm = local_version_manager.get_commit_metadata(
                    commit_metadata_event_uuid);

                String object_uuid = version_data.getObjectUuid();
                Delta delta = version_data.getDelta();
                local_version_manager.save_version_data(
                    object_uuid, delta, cm);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Error in disk local version replayer");
        }
    }

    @Override
    public EndpointInitializationHistory
        get_endpoint_initialization_history(String endpoint_uuid)
    {
        init_local_version_manager_if_unitialized();
        return local_version_manager.get_endpoint_initialization_history(
            endpoint_uuid);
    }

    /**
       @returns null if does not exist.
     */
    @Override
    public ObjectHistory get_full_object_history(String obj_uuid)
    {
        init_local_version_manager_if_unitialized();
        return local_version_manager.get_full_object_history(obj_uuid);
    }

    /**
       @returns null if does not exist.
     */
    @Override
    public EndpointConstructorObj get_endpoint_constructor_obj(
        String endpoint_constructor_obj_classname)
    {
        // note: not using internal local_version_manager here because
        // local_version_manager never got updated with endpoint
        // constructor objects.  However, the global version of
        // local_version_saver did when objects were initially
        // constructed.  Use this to answer
        // get_endpoint_constructor_obj queries.
        ILocalVersionSaver local_version_saver =
            VersioningInfo.instance.local_version_saver;
        return local_version_saver.get_endpoint_constructor_obj(
            endpoint_constructor_obj_classname);
    }
    
    /**
       @param lower_range --- null if should query from earliest
       record.

       @param upper_range --- null if should query to latest record.
       
       @returns null if does not exist.  Returns objecthistory object
       with no records if no records exist within range.
     */
    @Override
    public ObjectHistory get_ranged_object_history(
        String obj_uuid,Long lower_range, Long upper_range)
    {
        init_local_version_manager_if_unitialized();
        return local_version_manager.get_ranged_object_history(
            obj_uuid,lower_range,upper_range);
    }
}