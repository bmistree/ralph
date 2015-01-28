package ralph;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class MVar<ContentType>
{
    private final ReentrantLock mutex = new ReentrantLock();
    private final Condition condition = mutex.newCondition();

    private boolean internal_set = false;
    private ContentType internal_val = null;
    
    public void put(ContentType new_val)
    {
        mutex.lock();
        internal_set = true;
        internal_val = new_val;
        condition.signalAll();
        mutex.unlock();
    }

    /**
       Blocking.
     */
    public ContentType blocking_take()
    {
        mutex.lock();
        while(! internal_set)
        {
            try
            {
                condition.await();
            }
            catch(InterruptedException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Should never receive interrupted exceptions " +
                    "while in blocking_take.");                
            }
        }
        ContentType to_return = internal_val;
        mutex.unlock();
        return to_return;
    }
}