package ralph;
import java.util.concurrent.ArrayBlockingQueue;

public class WaitingElement <T,D>
    implements Comparable<WaitingElement<T,D>>
{
    
    //	# Each event has a priority associated with it.  This priority
    //	# can change when an event gets promoted to be boosted.  To
    //	# avoid the read/write conflicts this might cause, instead of
    //	# operating on an event's real-time priority, when trying to
    //	# acquire read and write locks on events, we initially request
    //	# their priorities and use them for the main body of that
    //	# operation.  This priority gets cached in WaitingElements and
    //	# can get updated, asynchronously, when an event requests
    //	# promotion.
    public String cached_priority; 
	
    public ActiveEvent event;
    private boolean read;
    DataWrapperConstructor<T,D> data_wrapper_constructor;
	
    // Acquire write lock and acquire read lock block reading this queu
    // if they cannot instantly acquire the write/read lock
    //# when add a waiting element, that waiting element's read or
    //# write blocks.  The way that it blocks is by listening at a
    //# threadsafe queue.  This is that queue.
    public ArrayBlockingQueue<DataWrapper<T,D>> queue = 
        new ArrayBlockingQueue<DataWrapper<T,D>>(Util.QUEUE_CAPACITIES);
	
    private boolean peered;
	
    /**
     * 
     * @param active_event
     * @param priority
     * 
     * @param read ---- True if the element that is waiting is waiting on 
     * a read lock (not a write lock)
     * 
     * 
     * @param data_wrapper_constructor
     * @param peered
     */
    public WaitingElement(
        ActiveEvent active_event,String _priority,boolean _read, 
        DataWrapperConstructor<T,D> _data_wrapper_constructor,boolean _peered)
    {
        event = active_event;
        cached_priority = _priority;
        read = _read;
        data_wrapper_constructor = _data_wrapper_constructor;
        peered = _peered;		
    }
	
    public boolean is_read()
    {
        return read;
    }
    public boolean is_write()
    {
        return !read;
    }
	

    /**
     * 
     * Called from within locked_obj's lock.

     Call to set_val and get_val are blocking.  They wait on a
     threadsafe queue, self.queue.  This method writes to
     threadsafe queue to unjam the waiting events.  
     * @return
     */
    public void unwait(MultiThreadedLockedObject multi_threaded_obj)
    {
        if (read)
        {
            // # read expects updated value returned in queue
            queue.add(multi_threaded_obj.val);
        }
        else
        {
            //# FIXME: it may be that we don't want to copy over initial
            //# value when acquiring a lock (eg., if we're just going to
            //# write over it anyways).  Add a mechanism for that?
            //
            //# update dirty val with value asked to write with
            multi_threaded_obj.dirty_val = 
                data_wrapper_constructor.construct(
                    (T)multi_threaded_obj.val.val,peered);
        }
    }
    
    public int compareTo(WaitingElement<T,D> o2) 
    {
        return cached_priority.compareTo(o2.cached_priority);
    }
}
