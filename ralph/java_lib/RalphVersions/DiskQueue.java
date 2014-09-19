package RalphVersions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.concurrent.ArrayBlockingQueue;

import com.google.protobuf.MessageLite;

import ralph.Util;

/**
   Maintains a queue of items that should get written to disk.
 */
public class DiskQueue<Type extends MessageLite>
    implements Runnable
{
    protected final ArrayBlockingQueue<Type> queue;

    // FIXME: should be finals, but do not want to deal with
    // exceptions when creating causing variables to not be
    // initialized.
    protected File file;
    protected FileOutputStream file_output_stream;

    public DiskQueue(int queue_capacity, String filename)
    {
        queue = new ArrayBlockingQueue<Type>(queue_capacity);

        try
        {
            file = new File(filename);
            file_output_stream = new FileOutputStream(file);
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

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                Type item = queue.take();
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
    
    public void blocking_enqueue_item(Type item_to_enqueue)
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
    
    protected void write_item(Type item_to_write)
    {
        try
        {
            item_to_write.writeDelimitedTo(file_output_stream);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("File error in DiskQueue");
        }
    }
}

