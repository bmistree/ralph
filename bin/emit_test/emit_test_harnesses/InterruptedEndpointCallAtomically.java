package emit_test_harnesses;

import ralph_emitted.BasicRalphJava.SetterGetter;
import ralph_emitted.InterruptedEndpointAtomicallyJava.InterruptedAtomicEndpoint;
import ralph.RalphGlobals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InterruptedEndpointCallAtomically
{
    final static int NUM_THREADS = 4;
    final static int NUM_TIMES_TO_TRY_TO_FORCE = 20;
    final static int NUM_MS_TO_WAIT_TO_COMPLETE = 40*1000;
    static AtomicBoolean was_interrupted = new AtomicBoolean(false);
    static AtomicBoolean had_exception = new AtomicBoolean(false);
    static AtomicInteger num_completed = new AtomicInteger(0);

    public enum ReturnCode
    {
        SUCCESS, FAILURE, UNKNOWN
    }
    
    public static void main(String[] args)
    {
        ReturnCode test_result = InterruptedEndpointCallAtomically.run_test();

        if (test_result == ReturnCode.SUCCESS)
            System.out.println("\nSUCCESS in InterruptedEndpointCallAtomically\n");
        else if (test_result == ReturnCode.FAILURE)
            System.out.println("\nFAILURE in InterruptedEndpointCallAtomically\n");
        else
            System.out.println(
                "\nUNKNOWN in InterruptedEndpointCallAtomically " +
                "(likely could not force interrupt).\n");
    }

    public static ReturnCode run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            SetterGetter internal_endpt =
                SetterGetter.create_single_sided(ralph_globals);

            double original_internal_number = 15;
            internal_endpt.set_number(new Double(original_internal_number));
            
            InterruptedAtomicEndpoint endpt =
                InterruptedAtomicEndpoint.create_single_sided(ralph_globals);

            endpt.set_endpt(internal_endpt);

            
            // start executor to perform simultaneous access
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

            TryToInterrupt tti = new TryToInterrupt(endpt);
            for (int i = 0; i < NUM_TIMES_TO_TRY_TO_FORCE; ++i)
                executor.execute(tti);

            int i = 0;
            int ms_to_sleep = 500;
            while (num_completed.get() != NUM_TIMES_TO_TRY_TO_FORCE)
            {
                i++;
                Thread.sleep(ms_to_sleep);
                int time_slept_ms = ms_to_sleep * i;
                if (time_slept_ms > NUM_MS_TO_WAIT_TO_COMPLETE)
                    break;
            }

            executor.shutdownNow();

            if (had_exception.get())
                return ReturnCode.FAILURE;

            // check if final value is what anticipated
            double expected_number =
                original_internal_number + ((double)NUM_TIMES_TO_TRY_TO_FORCE);
            if (internal_endpt.get_number().doubleValue() != expected_number)
                return ReturnCode.FAILURE;

            if (! was_interrupted.get())
                return ReturnCode.UNKNOWN;

            return ReturnCode.SUCCESS;
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return ReturnCode.FAILURE;
        }
    }

    public static class TryToInterrupt implements Runnable
    {
        private InterruptedAtomicEndpoint endpt;
        public TryToInterrupt(InterruptedAtomicEndpoint _endpt)
        {
            endpt = _endpt;
        }

        public void run()
        {
            try {
                double diff = 0;
                diff = endpt.long_event_increment().doubleValue();
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