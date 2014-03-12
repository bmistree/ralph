package RalphServiceActions;

import ralph.AtomicActiveEvent;
import java.util.concurrent.Future;
import ralph.ICancellableFuture;
import ralph.Util;
import java.util.concurrent.ExecutionException;

/**
   Listens for future to return.  Depending on result,
   speculative future either fails or succeeeds.
 */
public class LinkFutureBooleans extends ServiceAction
{
    private Future<Boolean> internal_boolean = null;
    private ICancellableFuture spec_future = null;
    public LinkFutureBooleans(
        Future<Boolean> internal_boolean,
        ICancellableFuture spec_future)
    {
        this.internal_boolean = internal_boolean;
        this.spec_future = spec_future;
    }

    public void run()
    {
        boolean result = false;
        try {
            result = internal_boolean.get();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Util.logger_assert(
                "Not considering the case of " +
                "an interrupted future.");
        } catch (ExecutionException ex) {
            ex.printStackTrace();
            Util.logger_assert(
                "Not considering the case of " +
                "an execution exception on future.");
        }

        if (result)
            spec_future.succeeded();
        else
            spec_future.failed();
    }
}
