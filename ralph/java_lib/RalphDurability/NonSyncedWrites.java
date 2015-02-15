package RalphDurability;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ralph_protobuffs.DurabilityProto.Durability;

public class NonSyncedWrites
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

    public void write_complete_operation(
        IDurabilityContext dc, boolean succeeded) throws IOException
    {
        Durability durability_msg = dc.complete_proto_buf(succeeded);
        durability_msg.writeDelimitedTo(buffered_file_output_stream);
    }
}
