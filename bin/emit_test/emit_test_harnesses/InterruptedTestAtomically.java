package emit_test_harnesses;

import ralph_emitted.InterruptedAtomicallyJava.InterruptedAtomic;
import ralph.RalphGlobals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InterruptedTestAtomically
{
    final static int NUM_THREADS = 10;
    final static int NUM_TIMES_TO_TRY_TO_FORCE = 50;
    final static int NUM_TO_INITIALLY_APPEND = 50;
    final static int NUM_MS_TO_WAIT_TO_COMPLETE = 400* 1000;
    static AtomicBoolean was_interrupted; 
    static AtomicBoolean had_exception;
    static AtomicInteger num_completed;

    public enum ReturnCode
    {
        SUCCESS, FAILURE, UNKNOWN
    }
    
    public static void main(String[] args)
    {
        ReturnCode test_result = InterruptedTestAtomically.run_test();

        if (test_result == ReturnCode.SUCCESS)
            System.out.println("\nSUCCESS in InterruptedTestAtomically\n");
        else if (test_result == ReturnCode.FAILURE)
            System.out.println("\nFAILURE in InterruptedTestAtomically\n");
        else
            System.out.println(
                "\nUNKNOWN in InterruptedTestAtomically (likely could not force interrupt).\n");
    }

    public static ReturnCode run_test()
    {
        ReturnCode status = ReturnCode.SUCCESS;
        try
        {
            InterruptedAtomic endpt =
                InterruptedAtomic.create_single_sided(new RalphGlobals());

            // failure report takes precedence over unknown report
            // takes precedence over success report
            ReturnCode unnested_return_code = long_event(endpt,false);
            if (unnested_return_code != ReturnCode.SUCCESS)
            {
                if (status != ReturnCode.FAILURE)
                    status = unnested_return_code;
            }

            ReturnCode nested_return_code = long_event(endpt,true);
            if (nested_return_code != ReturnCode.SUCCESS)
            {
                if (status != ReturnCode.FAILURE)
                    status = nested_return_code;
            }
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return ReturnCode.FAILURE;
        }

        return status;
        
    }

    public static ReturnCode long_event(
        InterruptedAtomic endpt,boolean nested) throws Exception
    {
        was_interrupted = new AtomicBoolean(false);
        had_exception = new AtomicBoolean(false);
        num_completed = new AtomicInteger(0);

        endpt.clear_list();
        for (int i = 0; i < NUM_TO_INITIALLY_APPEND; ++i)
        {
            double diff = 0;
            if (nested)
                diff = endpt.nested_long_event_and_append().doubleValue();
            else
                diff = endpt.long_event_and_append().doubleValue();
            
            if (diff != 0.)
                return ReturnCode.FAILURE;
        }

        if (! endpt.list_size().equals((double)NUM_TO_INITIALLY_APPEND))
            return ReturnCode.FAILURE;

        ExecutorService executor = Executors.newFixedThreadPool(
            NUM_THREADS,
            new ThreadFactory()
            {
                // each thread created is a daemon
                public Thread newThread(Runnable r)
                {
                    Thread t=new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

        TryToInterrupt tti = new TryToInterrupt(endpt,nested);
        for (int i = 0; i < NUM_TIMES_TO_TRY_TO_FORCE; ++i)
            executor.execute(tti);

        int i = 0;
        int ms_to_sleep = 500;
        while (num_completed.get() != NUM_TIMES_TO_TRY_TO_FORCE)
        {
            i++;
            // System.out.println(
            //     Integer.toString(num_completed.get()) + " of " +
            //     Integer.toString(NUM_TIMES_TO_TRY_TO_FORCE));
            Thread.sleep(ms_to_sleep);
            int time_slept_ms = ms_to_sleep * i;
            if (time_slept_ms > NUM_MS_TO_WAIT_TO_COMPLETE)
                break;
        }

        executor.shutdownNow();

        if (had_exception.get())
            return ReturnCode.FAILURE;

        if (endpt.list_size() != ((double) (NUM_TO_INITIALLY_APPEND + NUM_TIMES_TO_TRY_TO_FORCE)))
            return ReturnCode.FAILURE;

        if (! was_interrupted.get())
            return ReturnCode.UNKNOWN;
        
        return ReturnCode.SUCCESS;
    }


    public static class TryToInterrupt implements Runnable
    {
        private InterruptedAtomic endpt;
        private boolean nested;
        public TryToInterrupt(InterruptedAtomic _endpt,boolean _nested)
        {
            endpt = _endpt;
            nested = _nested;
        }

        public void run()
        {
            try {
                double diff = 0;
                if (nested)
                    diff = endpt.nested_long_event_and_append().doubleValue();
                else
                    diff = endpt.long_event_and_append().doubleValue();
                
                num_completed.getAndIncrement();
                
                if (diff != 0.)
                    was_interrupted.set(true);
            } catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
            
        }
    }
}