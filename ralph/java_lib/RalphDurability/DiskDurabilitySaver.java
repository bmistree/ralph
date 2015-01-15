package RalphDurability;

import java.util.List;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.IOException;

import ralph.Util;

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
    
    public DiskDurabilitySaver (String log_filename)
    {
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
            Util.logger_assert("Could not initialize logging output stream.");
        }
        return to_return;
    }
    
    @Override
    public void prepare_operation(DurabilityContext dc)
    {
        Durability durability_msg = dc.prepare_proto_buf();
        write_durability_msg(durability_msg);
    }

    @Override
    public void complete_operation(DurabilityContext dc, boolean succeeded)
    {
        Durability durability_msg = dc.complete_proto_buf(succeeded);
        if (durability_msg != null)
            write_durability_msg(durability_msg);
    }
    
    private void write_durability_msg(Durability durability_msg)
    {
        try
        {
            durability_msg.writeDelimitedTo(f_output);
            f_channel.force(false);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("IOException in prepare");
        }
    }
}