package ralph;
import java.util.concurrent.ArrayBlockingQueue;

import RalphDataWrappers.DataWrapperFactory;
import RalphDataWrappers.DataWrapper;

public class WaitingElement <T>
    implements Comparable<WaitingElement<T>>
{
    
    // Each event has a priority associated with it.  This priority
    // can change when an event gets promoted to be boosted.  To
    // avoid the read/write conflicts this might cause, instead of
    // operating on an event's real-time priority, when trying to
    // acquire read and write locks on events, we initially request
    // their priorities and use them for the main body of that
    // operation.  This priority gets cached in WaitingElements and
    // can get updated, asynchronously, when an event requests
    // promotion.
    public String cached_priority; 
	
    public ActiveEvent event;
    private boolean read;
    DataWrapperFactory<T> data_wrapper_constructor;
	
    // Acquire write lock and acquire read lock block reading this
    // queue if they cannot instantly acquire the write/read lock when
    // add a waiting element, that waiting element's read or write
    // blocks.  The way that it blocks is by listening at a threadsafe
    // queue.  This is that queue.
    public static class UnwaitElement<T>
    {
        public boolean successful;
        // can be null
        public DataWrapper<T> result;

        /**
           @param _result --- If null, means that operation was not
           successful and waiter should throw backout.
         */
        public UnwaitElement(DataWrapper<T> _result)
        {
            successful = (_result != null);
            result = _result;
        }
    }

    // FIXME: Really just need a synchronized single-element channel
    // with backout-always-wins semantics
    public ArrayBlockingQueue<UnwaitElement> queue = 
        new ArrayBlockingQueue<UnwaitElement>(Util.QUEUE_CAPACITIES);

    
    private boolean log_changes;
	
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
     * @param log_changes
     */
    public WaitingElement(
        ActiveEvent active_event,String _priority,boolean _read, 
        DataWrapperFactory<T> _data_wrapper_constructor,boolean _log_changes)
    {
        event = active_event;
        cached_priority = _priority;
        read = _read;
        data_wrapper_constructor = _data_wrapper_constructor;
        log_changes = _log_changes;		
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
    public void unwait(AtomicObject multi_threaded_obj)
    {
        if (read)
        {
            // read expects updated value returned in queue            
            queue.add(new UnwaitElement(multi_threaded_obj.val));
        }
        else
        {
            // FIXME: it may be that we don't want to copy over initial
            // value when acquiring a lock (eg., if we're just going to
            // write over it anyways).  Add a mechanism for that?
            //
            // update dirty val with value asked to write with
            multi_threaded_obj.dirty_val =
                data_wrapper_constructor.construct(
                    (T)multi_threaded_obj.val.val,log_changes);
            queue.add(new UnwaitElement(multi_threaded_obj.dirty_val));
        }
    }
    
    /**
       Called from within lcoked_obj's lock.
     */
    public void unwait_fail(AtomicObject multi_threaded_obj)
    {
        queue.add(new UnwaitElement(null));
    }
    
    public int compareTo(WaitingElement<T> o2) 
    {
        if (o2.cached_priority.equals(cached_priority))
            return 0;

        if (EventPriority.gte_priority(cached_priority,o2.cached_priority))
            return -1;

        return 1;
    }
}
