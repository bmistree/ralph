package RalphDurability;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ralph.EndpointConstructorObj;
import ralph.Util;

import ralph_protobuffs.DurabilityProto.Durability;


public class NonSyncedWrites implements IDurabilitySaver
{
    protected final static int BUFFERED_WRITER_BUFFER_SIZE_BYTES = 128*1024;

    protected File file;
    protected BufferedOutputStream buffered_file_output_stream;

    public NonSyncedWrites(String filename) throws IOException
    {
        file = new File(filename);
        buffered_file_output_stream =
            new BufferedOutputStream(
                new FileOutputStream(file),
                BUFFERED_WRITER_BUFFER_SIZE_BYTES);
    }

    @Override
    public synchronized void complete_operation(
        IDurabilityContext dc, boolean succeeded)
    {
        Durability durability_msg = dc.complete_proto_buf(succeeded);
        try
        {
            durability_msg.writeDelimitedTo(buffered_file_output_stream);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Could not write complete_operation");
        }
    }

    @Override
    public synchronized void prepare_operation(IDurabilityContext dc)
    {
        try
        {
            Durability durability_msg = dc.prepare_proto_buf();
            // replayed durabilities return nulls.
            if (durability_msg != null)
                durability_msg.writeDelimitedTo(buffered_file_output_stream);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Could not write prepare_operation");
        }
    }

    @Override
    public synchronized void ensure_logged_endpt_constructor(
        EndpointConstructorObj endpt_constructor_obj)
    {
        Durability msg =
            DurabilityContext.endpt_constructor_durability_constructor(
                endpt_constructor_obj);
        try
        {
            msg.writeDelimitedTo(buffered_file_output_stream);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Could not write ensure_logged_endpt_constructor");
        }
    }

    /**************** Factory for DiskDurabilitySaver ******/
    private static class ConstructorFactory
        implements IDurabilitySaverFactory
    {
        @Override
        public IDurabilitySaver construct(String filename)
        {
            return new DiskDurabilitySaver(filename,false);
        }
    }

    public final static ConstructorFactory CONSTRUCTOR_FACTORY =
        new ConstructorFactory();
}
