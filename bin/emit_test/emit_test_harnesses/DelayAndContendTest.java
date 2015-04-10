package emit_test_harnesses;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import ralph_emitted.DelayAndContendJava.DelayAndContend;
import ralph.RalphGlobals;

public class DelayAndContendTest
{
    //private final static int NUM_CONTENDING_METHODS = 1000;
    private final static int NUM_CONTENDING_METHODS = 600;
    private final static AtomicInteger num_executed = new AtomicInteger(0);
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);
    private final static int TIMEOUT_MS = 30*1000;
    
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DelayAndContendTest\n");
        else
            System.out.println("\nFAILURE in DelayAndContendTest\n");
    }

    public static boolean run_test()
    {        
        try
        {
            ExecutorService executor = Executors.newCachedThreadPool();
            DelayAndContend endpt =
                DelayAndContend.create_single_sided(new RalphGlobals());
            
            Task t = new Task(endpt);
            for (int i = 0; i < NUM_CONTENDING_METHODS; ++i)
                executor.execute(t);

            executor.shutdown();

            int num_half_seconds = 0;
            while (! executor.isTerminated())
            {
                Thread.sleep(500);
                ++num_half_seconds;

                if (num_half_seconds*500 > TIMEOUT_MS)
                    throw new Exception("Operations timed out");
            }
            
            if (num_executed.get() != NUM_CONTENDING_METHODS)
                return false;

            if (had_exception.get())
                return false;
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }


    private static class Task implements Runnable
    {
        private final DelayAndContend delay_and_contend;

        public Task(DelayAndContend delay_and_contend)
        {
            this.delay_and_contend = delay_and_contend;
        }
        
        @Override
        public void run()
        {
            try
            {
                delay_and_contend.contending_method();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
            finally
            {
                num_executed.getAndIncrement();
            }
        }
    }
}