package ralph;

import java.util.concurrent.Future;
import java.util.ArrayList;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ParallelBlockConstructor<E>
{
    protected VariableStack vstack = null;
    protected LockedActiveEvent active_event = null;

    public ParallelBlockConstructor(
        VariableStack _vstack,LockedActiveEvent _active_event)
    {
        vstack = _vstack;
        active_event = _active_event;
    }

    public abstract ParallelBlock<E> produce_par_block ();

    /**
       Blocks until all are complete or there is an uncaught exception
       in one.
     */
    public void exec_par(Iterable<E> to_exec_over)
        throws ApplicationException, BackoutException, NetworkException,StoppedException
    {
        // Mimics behavior of ForkJoinPool, but ForkJoinPool requires
        // version 1.7 of Java, and Android does not completely support
        ArrayList<Future<Integer>> waiting_futures =
            new ArrayList<Future<Integer>> ();

        for (E arg_to_run_with : to_exec_over)
        {
            ParallelBlock<E> item_par_block = produce_par_block();
            item_par_block.set_to_run_on(arg_to_run_with);
            waiting_futures.add(
                SingletonParallelExecutors.submit(item_par_block));
        }

        // join on all the operations submitted.
        try
        {
            for (Future<Integer> f : waiting_futures)
                check_throw_call_result_error(f.get());
        }
        catch (InterruptedException _ex)
        {
            Util.logger_assert(
                "\nNot handling interrupted exception while running " +
                "parallel block.\n");
        }
        catch (ExecutionException _ex)
        {
            Util.logger_assert(
                "\nDid not catch execution exception in parallel block\n");
        }

    }
    /**
       Depending on the value of call_result, throw an error, or do
       nothing.
     */
    private void check_throw_call_result_error(Integer call_result)
        throws ApplicationException, BackoutException, NetworkException,StoppedException
    {
        if (call_result == ParallelBlock.CALL_NO_ERROR)
            return;

        else if (call_result == ParallelBlock.CALL_BACKOUT_EXCEPTION)
            throw new BackoutException();

        else if (call_result == ParallelBlock.CALL_APPLICATION_EXCEPTION)
            throw new ApplicationException("From parallel block");

        else if (call_result == ParallelBlock.CALL_NETWORK_EXCEPTION)
            throw new ApplicationException("From parallel block");

        else if (call_result == ParallelBlock.CALL_STOPPED_EXCEPTION)
            throw new StoppedException();

        // DEBUG
        else
            Util.logger_assert("\n\nUnknown exception\n\n");
        // END DEBUG
    }
}

