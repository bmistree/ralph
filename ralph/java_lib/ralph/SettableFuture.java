package ralph;

import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;

public class SettableFuture<InternalType> implements Future<InternalType>
{
    private final Lock mutex = new ReentrantLock();
    private final Condition cond = mutex.newCondition(); 
    private InternalType internal = null;
    private boolean has_been_set = false;
    
    public SettableFuture ()
    {}

    /**
       @param new_val --- If internal val has not already been set,
       then set it and wake up all listeners.
       
       @returns true if set correctly; false if has already been set.
     */
    public boolean set(InternalType new_val)
    {
        mutex.lock();
        try
        {
            if (has_been_set)
                return false;

            has_been_set = true;
            internal = new_val;
            cond.signalAll();
            return true;
        }
        finally
        {
            mutex.unlock();
        }
    }
    
    @Override
    public boolean cancel (boolean may_interrupt_if_running)
    {
        return false;
    }
    
    @Override
    public InternalType get()
    {
        mutex.lock();
        try
        {
            while (! has_been_set)
                cond.awaitUninterruptibly();
            return internal;
        }
        finally
        {
            mutex.unlock();
        }
    }

    @Override
    public InternalType get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
        
    {
        mutex.lock();
        try
        {
            if (! has_been_set)
            {
                // throws InterruptedException
                boolean timed_out = cond.await(timeout, unit);
                if (timed_out)
                    throw new TimeoutException();
            }
            return internal;
        }
        finally
        {
            mutex.unlock();
        }
    }

    @Override
    public boolean isCancelled()
    {
        return false;
    }
    
    @Override
    public boolean isDone()
    {
        mutex.lock();
        try
        {
            return has_been_set;
        }
        finally
        {
            mutex.unlock();
        }
    }
}