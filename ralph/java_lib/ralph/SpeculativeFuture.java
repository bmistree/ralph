package ralph;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

public class SpeculativeFuture implements ICancellableFuture
{
    private final ReentrantLock rlock = new ReentrantLock();
    private final Condition cond = rlock.newCondition();
    private boolean has_been_set = false;
    private boolean to_return = false;
    public ActiveEvent event = null;
    private final boolean read_only;
    
    public SpeculativeFuture(ActiveEvent event, boolean read_only)
    {
        this.event = event;
        this.read_only = read_only;
    }

    /** ICancellableFuture interface */
    @Override
    public void failed()
    {
        set(false);
    }
    @Override
    public void succeeded()
    {
        set(true);
    }

    public boolean is_read_only()
    {
        return read_only;
    }
    
    private void set(boolean to_set_to)
    {
        rlock.lock();
        has_been_set = true;
        to_return = to_set_to;
        cond.signalAll();
        rlock.unlock();
    }

    /** Future<Boolean> interface */
    @Override
    public Boolean get()
    {
        rlock.lock();
        while(! has_been_set)
        {
            try
            {
                cond.await();
            }
            catch (InterruptedException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "\nShould be handling interrupted exception\n");
            }
        }
        rlock.unlock();
        return to_return;
    }

    @Override
    public Boolean get(long timeout, TimeUnit unit)
    {
        Util.logger_assert(
            "SpeculativeFuture does not support timed gets");
        return null;
    }

    @Override
    public boolean isCancelled()
    {
        Util.logger_assert(
            "SpeculativeFuture does not support isCancelled");
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        Util.logger_assert(
            "SpeculativeFuture does not support cancel");
        return false;
    }

    @Override
    public boolean isDone()
    {
        boolean to_return;
        rlock.lock();
        to_return = has_been_set;
        rlock.unlock();
        return to_return;
    }        
}