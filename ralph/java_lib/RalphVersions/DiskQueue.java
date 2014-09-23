package RalphVersions;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedWriter;
import java.util.concurrent.ArrayBlockingQueue;

import ralph_local_version_protobuffs.VersionSaverMessagesProto.VersionSaverMessages;
import com.google.protobuf.MessageLite;

import ralph.Util;

/**
   Maintains a queue of items that should get written to disk.
 */
//public class DiskQueue<Type extends MessageLite>
public class DiskQueue implements Runnable
{
    protected final ArrayBlockingQueue<VersionSaverMessages.Builder> queue;

    protected final static int BUFFERED_WRITER_BUFFER_SIZE_BYTES = 128*1024;
    
    // FIXME: should be finals, but do not want to deal with
    // exceptions when creating causing variables to not be
    // initialized.
    protected File file;
    //protected FileOutputStream file_output_stream;
    protected BufferedOutputStream buffered_file_output_stream;

    public DiskQueue(int queue_capacity, String filename)
    {
        queue = new ArrayBlockingQueue<VersionSaverMessages.Builder>(queue_capacity);

        try
        {
            file = new File(filename);
            buffered_file_output_stream =
                new BufferedOutputStream(
                    new FileOutputStream(file),
                    BUFFERED_WRITER_BUFFER_SIZE_BYTES);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("File error in disk queue.");
        }

        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    public void flush()
    {
        try
        {
            buffered_file_output_stream.flush();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Not handling flush exception for DiskQueue.");
        }
    }
    
    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                VersionSaverMessages.Builder item = queue.take();
                write_item(item);
            }
            catch (InterruptedException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Unexpected interrupted exception in disk queue thread.");
            }
        }
    }
    
    public void blocking_enqueue_item(VersionSaverMessages.Builder item_to_enqueue)
    {
        // blocks until available space in queue to put item.
        try
        {
            queue.put(item_to_enqueue);
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Unexpected interrupted exception in disk queue enqueue");
        }
    }
    
    protected void write_item(VersionSaverMessages.Builder item_to_write)
    {
        try
        {
            item_to_write.build().writeDelimitedTo(buffered_file_output_stream);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("File error in DiskQueue");
        }
    }
}

