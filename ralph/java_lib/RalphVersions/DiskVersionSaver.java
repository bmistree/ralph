package RalphVersions;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.io.File;

import ralph.CommitMetadata;
import ralph.EndpointConstructorObj;
import ralph.EnumConstructorObj;
import ralph.Util;

import ralph_protobuffs.VersionSaverMessagesProto.VersionSaverMessages;
import ralph_protobuffs.DeltaProto.Delta;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;


public class DiskVersionSaver implements IVersionSaver
{
    // key is the classname of the endpoint constructor object.
    private final Map<String,EndpointConstructorObj> endpoint_constructor_map =
        new HashMap<String,EndpointConstructorObj>();

    // key is the classname of the enum constructor object.
    private final Map<String,EnumConstructorObj> enum_constructor_map =
        new HashMap<String,EnumConstructorObj>();
    
    private final List<DiskQueue> disk_queue_list =
        new ArrayList<DiskQueue>();

    private final AtomicInteger disk_queue_counter = new AtomicInteger(0);

    /**
       @param message_buffer_capacity --- Number of messages can
       buffer in queues before writing to disk.

       @param folder_name --- 
     */
    public DiskVersionSaver(
        int message_buffer_capacity, String folder_name,int num_log_files)
    {
        try
        {
            File folder = new File(folder_name);
            if (!folder.exists())
                folder.mkdir();

            List<String> filenames =
                DiskVersionSaver.generate_logging_filenames(num_log_files);
            for (String filename : filenames)
            {
                disk_queue_list.add(
                    new DiskQueue(
                        message_buffer_capacity,folder_name,filename));
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Exception in logger creating files");
        }
    }

    public static List<String> generate_logging_filenames(int num_log_files)
    {
        List<String> to_return = new ArrayList<String>();
        for (int i = 0; i < num_log_files; ++i)
        {
            String filename = Integer.toString(i) + ".bin";
            to_return.add(filename);
        }
        return to_return;
    }
    

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
        for (DiskQueue disk_queue : disk_queue_list)
            disk_queue.flush();
    }


    protected DiskQueue get_disk_queue()
    {
        int unmodded_disk_queue_index = disk_queue_counter.incrementAndGet();
        DiskQueue disk_queue = disk_queue_list.get(
            unmodded_disk_queue_index % disk_queue_list.size());
        return disk_queue;
    }
    
    @Override
    public void save_commit_metadata(CommitMetadata commit_metadata)
    {
        DiskQueue disk_queue = get_disk_queue();
        
        VersionSaverMessages.CommitMetadata.Builder cm_builder =
            VersionUtil.commit_metadata_message_builder(commit_metadata);
        VersionSaverMessages.Builder vsm = VersionSaverMessages.newBuilder();
        vsm.setCommitMetadata(cm_builder);
        disk_queue.blocking_enqueue_item(vsm);
    }
    
    @Override
    public void save_version_data(
        String object_uuid, Delta delta, CommitMetadata commit_metadata)
    {
        DiskQueue disk_queue = get_disk_queue();
        
        VersionSaverMessages.VersionData.Builder vd_builder =
            VersionUtil.version_data_message_builder(
                object_uuid,delta,commit_metadata);
        
        VersionSaverMessages.Builder vsm = VersionSaverMessages.newBuilder();
        vsm.setVersionData(vd_builder);
        disk_queue.blocking_enqueue_item(vsm);
    }
    
    @Override
    public void save_endpoint_global_mapping(
        String variable_name, String object_uuid,String endpoint_uuid,
        String endpoint_constructor_class_name,long local_lamport_time)
    {
        DiskQueue disk_queue = get_disk_queue();
        
        VersionSaverMessages.EndpointGlobalMapping.Builder builder =
            VersionUtil.endpoint_global_mapping_message_builder(
                variable_name, object_uuid, endpoint_uuid,
                endpoint_constructor_class_name, local_lamport_time);
        
        VersionSaverMessages.Builder vsm = VersionSaverMessages.newBuilder();
        vsm.setEndpointGlobalMapping(builder);
        disk_queue.blocking_enqueue_item(vsm);
    }
    
    @Override
    public void save_object_constructor(
        String object_uuid, ObjectContents obj_contents)
    {
        DiskQueue disk_queue = get_disk_queue();
        
        VersionSaverMessages.ObjectConstructor.Builder builder =
            VersionUtil.object_constructor_message_builder(
                object_uuid,obj_contents);
        
        VersionSaverMessages.Builder vsm = VersionSaverMessages.newBuilder();
        vsm.setObjectConstructor(builder);
        disk_queue.blocking_enqueue_item(vsm);
    }
    
    @Override
    public void save_endpoint_constructor_obj(
        EndpointConstructorObj endpoint_constructor_obj)
    {
        endpoint_constructor_map.put(
            endpoint_constructor_obj.getClass().getName(),
            endpoint_constructor_obj);
    }
    
    @Override
    public void save_enum_constructor_obj(
        EnumConstructorObj enum_constructor_obj)
    {
        enum_constructor_map.put(
            enum_constructor_obj.getClass().getName(),
            enum_constructor_obj);
    }
    
    @Override
    public void close_versioned_object(String object_uuid)
    {
    }
}