package emit_test_harnesses;

import emit_test_package.InterruptedAtomically.InterruptedAtomic;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InterruptedTestAtomically
{
    final static int NUM_THREADS = 50;
    final static int NUM_TIMES_TO_TRY_TO_FORCE = 500;
    static AtomicBoolean was_interrupted = new AtomicBoolean(false);
    static AtomicBoolean had_exception = new AtomicBoolean(false);
    static AtomicInteger num_completed = new AtomicInteger(0);

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
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            InterruptedAtomic endpt = new InterruptedAtomic(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            int NUM_TO_INITIALLY_APPEND = 500;
            for (int i = 0; i < NUM_TO_INITIALLY_APPEND; ++i)
            {
                double diff = endpt.long_event_and_append().doubleValue();
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

            TryToInterrupt tti = new TryToInterrupt(endpt);
            for (int i = 0; i < NUM_TIMES_TO_TRY_TO_FORCE; ++i)
                executor.execute(tti);

            while (num_completed.get() != NUM_TIMES_TO_TRY_TO_FORCE)
                Thread.sleep(1000);
            

            if (endpt.list_size() != ((double) (NUM_TO_INITIALLY_APPEND + NUM_TIMES_TO_TRY_TO_FORCE)))
                return ReturnCode.FAILURE;                

            if (had_exception.get())
                return ReturnCode.FAILURE;

            if (was_interrupted.get())
                return ReturnCode.UNKNOWN;
            
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return ReturnCode.FAILURE;
        }
        return ReturnCode.SUCCESS;
    }


    public static class TryToInterrupt implements Runnable
    {
        private InterruptedAtomic endpt;
        public TryToInterrupt(InterruptedAtomic _endpt)
        {
            endpt = _endpt;
        }

        public void run()
        {
            num_completed.getAndIncrement();
            try {
                double diff = endpt.long_event_and_append().doubleValue();
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