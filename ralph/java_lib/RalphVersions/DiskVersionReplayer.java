package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ralph.Util;
import ralph.EndpointConstructorObj;
import ralph.EnumConstructorObj;
import ralph.CommitMetadata;
import ralph.VersioningInfo;

import ralph_protobuffs.DeltaProto.Delta;
import ralph_protobuffs.VersionSaverMessagesProto.VersionSaverMessages;


public class DiskVersionReplayer implements IVersionReplayer
{
    protected final String folder_name;
    protected final List<String> filename_list;
    
    protected boolean has_been_initialized = false;
    protected InMemoryVersionManager version_manager = null;
    
    public DiskVersionReplayer (String _folder_name, int num_files)
    {
        folder_name = _folder_name;
        filename_list =
            DiskVersionSaver.generate_logging_filenames(num_files);
    }


    /**
       Loads a single file.
       
       @returns --- a list of verion datas that should only be applied
       after all files have been loaded.
     */
    protected List<VersionSaverMessages.VersionData> read_in_single_file(
        String folder_name, String filename) throws IOException
    {
        // have to buffer registering VersionData-s because need
        // to pass in associated CommitMetadata object.
        List<VersionSaverMessages.VersionData> buffered_version_datas =
            new ArrayList<VersionSaverMessages.VersionData>();

        File file = new File(folder_name,filename);
        FileInputStream file_input_stream = new FileInputStream(file);
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
                version_manager.save_commit_metadata(cm);
            }
            else if (vsm.hasEndpointGlobalMapping())
            {
                VersionSaverMessages.EndpointGlobalMapping
                    endpt_global_mapping_msg =
                    vsm.getEndpointGlobalMapping();
                version_manager.save_endpoint_global_mapping(
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
                version_manager.save_object_constructor(
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

        return buffered_version_datas;
    }
    
    
    /**
       Essentially, load all file data into version_manager, and
       use that to respond to queries.
     */
    protected void init_version_manager_if_unitialized()
    {
        if (version_manager != null)
            return;
        
        version_manager = new InMemoryVersionManager();
        try
        {
            List<VersionSaverMessages.VersionData> buffered_version_datas =
                new ArrayList<VersionSaverMessages.VersionData>();

            for (String filename : filename_list)
            {
                buffered_version_datas.addAll(
                    read_in_single_file(folder_name,filename));
            }
            
            // now replay version data messages
            for (VersionSaverMessages.VersionData version_data : 
                     buffered_version_datas)
            {
                String commit_metadata_event_uuid =
                    version_data.getCommitMetadataEventUuid();
                CommitMetadata cm = version_manager.get_commit_metadata(
                    commit_metadata_event_uuid);
                
                //// DEBUG
                if (cm == null)
                    Util.logger_assert("Unknown commit metadata to rebuild");
                //// END DEBUG

                String object_uuid = version_data.getObjectUuid();
                Delta delta = version_data.getDelta();
                version_manager.save_version_data(
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
        init_version_manager_if_unitialized();
        return version_manager.get_endpoint_initialization_history(
            endpoint_uuid);
    }

    /**
       @returns null if does not exist.
     */
    @Override
    public ObjectHistory get_full_object_history(String obj_uuid)
    {
        init_version_manager_if_unitialized();
        return version_manager.get_full_object_history(obj_uuid);
    }

    /**
       @returns null if does not exist.
     */
    @Override
    public EndpointConstructorObj get_endpoint_constructor_obj(
        String endpoint_constructor_obj_classname)
    {
        // note: not using internal version_manager here because
        // version_manager never got updated with endpoint
        // constructor objects.  However, the global version of
        // version_saver did when objects were initially
        // constructed.  Use this to answer
        // get_endpoint_constructor_obj queries.
        IVersionSaver version_saver =
            VersioningInfo.instance.version_saver;
        return version_saver.get_endpoint_constructor_obj(
            endpoint_constructor_obj_classname);
    }

    @Override
    public EnumConstructorObj get_enum_constructor_obj(
        String enum_constructor_obj_classname)
    {
        // note: not using internal version_manager here because
        // version_manager never got updated with endpoint
        // constructor objects.  However, the global version of
        // version_saver did when objects were initially
        // constructed.  Use this to answer
        // get_endpoint_constructor_obj queries.
        IVersionSaver version_saver =
            VersioningInfo.instance.version_saver;
        return version_saver.get_enum_constructor_obj(
            enum_constructor_obj_classname);
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
        init_version_manager_if_unitialized();
        return version_manager.get_ranged_object_history(
            obj_uuid,lower_range,upper_range);
    }
}