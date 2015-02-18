package RalphDurability;

import java.util.List;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.IOException;

import ralph.Util;
import ralph.EndpointConstructorObj;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;

/**
   Logs all messages to disk.  Note: don't have atomic file
   operations.  But whenever we write a protobuff, we first write the
   protobuff's size.  For partial writes, just keep shifting until can
   read full file.
 */
public class DiskDurabilitySaver implements IDurabilitySaver
{
    private final FileChannel f_channel;
    private final FileOutputStream f_output;
    // key is endpoint constructor object's canonical name; value
    // doesn't matter.
    private final LRUCache<String,Boolean> constructor_obj_index;


    public DiskDurabilitySaver (String log_filename)
    {
        this(log_filename,true);
    }

    public DiskDurabilitySaver (
        String log_filename, boolean handle_constructor_cache)
    {
        if (handle_constructor_cache)
            constructor_obj_index = new LRUCache<String,Boolean>();
        else
            constructor_obj_index = null;
        
        f_output = initialize_f_output(log_filename);
        if (f_output == null)
            f_channel = null;
        else
            f_channel = f_output.getChannel();
    }
    
    private FileOutputStream initialize_f_output(String log_filename)
    {
        FileOutputStream to_return = null;
        try
        {
            to_return = new FileOutputStream(log_filename);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Could not initialize logging output stream.");
        }
        return to_return;
    }
    
    @Override
    public void prepare_operation(IDurabilityContext dc)
    {
        Durability durability_msg = dc.prepare_proto_buf();
        // replayed durabilities return nulls.
        if (durability_msg != null)
            write_durability_msg(durability_msg,true);
    }

    @Override
    public void complete_operation(IDurabilityContext dc, boolean succeeded)
    {
        Durability durability_msg = dc.complete_proto_buf(succeeded);
        if (durability_msg != null)
            write_durability_msg(durability_msg,false);
    }
    
    private void write_durability_msg(
        Durability durability_msg,boolean should_fsync)
    {
        try
        {
            durability_msg.writeDelimitedTo(f_output);
            if (should_fsync)
                f_channel.force(false);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("IOException in prepare");
        }
    }
    
    @Override
    public void ensure_logged_endpt_constructor(
        EndpointConstructorObj endpt_constructor_obj)
    {
        boolean should_log_to_disk = false;
        if (endpt_constructor_obj != null)
        {
            // require synchronized access to lru cache
            synchronized(this)
            {
                String const_name = endpt_constructor_obj.get_canonical_name();
                Boolean value =
                    constructor_obj_index.get(const_name);

                if (value == null)
                {
                    should_log_to_disk = true;
                    constructor_obj_index.put(const_name,true);
                }
            }
        }

        if (should_log_to_disk)
        {
            Durability msg =
                DurabilityContext.endpt_constructor_durability_constructor(
                    endpt_constructor_obj);
            
            write_durability_msg(msg,true);
        }
    }
}