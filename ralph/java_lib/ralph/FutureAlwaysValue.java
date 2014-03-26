package ralph;

import java.util.concurrent.TimeUnit;

/**
   AtomicActiveEvents request AtomicObjects to enter first phase
   commit.  This triggers atomic objects to perform any work they
   need to ensure their changes are valid/can be pushed.

   Return a future, which the AtomicActiveEvent can check to
   ensure that the change went through.  The future below will
   always return true.  Subclasses, when they override this object
   may override to use a different future that actually does work.
*/
public class FutureAlwaysValue implements ICancellableFuture
{
    private static final Boolean TRUE_BOOLEAN = new Boolean(true);
    private static final Boolean FALSE_BOOLEAN = new Boolean(false);
    
    private Boolean what_to_return = null;
        
    private FutureAlwaysValue(boolean _what_to_return)
    {
        if (_what_to_return)
            what_to_return = TRUE_BOOLEAN;
        else
            what_to_return = FALSE_BOOLEAN;
    }

    @Override
    public void failed()
    {}
    
    @Override
    public void succeeded()
    {}
    
    @Override
    public Boolean get()
    {
        return what_to_return;
    }

    @Override
    public Boolean get(long timeout, TimeUnit unit)
    {
        return get();
    }

    @Override
    public boolean isDone()
    {
        return true;
    }

    @Override
    public boolean isCancelled()
    {
        Util.logger_assert(
            "FutureAlways does not support isCancelled");
        return false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        Util.logger_assert(
            "FutureAlways does not support cancel");
        return false;
    }

    public static final FutureAlwaysValue ALWAYS_TRUE_FUTURE =
        new FutureAlwaysValue(true);
    public static final FutureAlwaysValue ALWAYS_FALSE_FUTURE =
        new FutureAlwaysValue(false);
}
    
