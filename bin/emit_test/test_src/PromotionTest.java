package emit_test_harnesses;

import emit_test_package.Promotion.PromoterEndpoint;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;
import ralph.Endpoint;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
   Reproduced from top of promotion.rph file.
   
   Want to ensure that get quality of service amongst endpoints.  Way to test
   this is to create two, connected LongAndWrite endpoints, A and B.  Issue a
   bunch of call_from_external calls on A (with arg true).  Before these
   complete, issue a bunch of call_from_external calls on B (with arg false).

   This means that we'll increment my_number a bunch on A and decrement
   my_number a bunch on B.  

   Put results of each call into a threadsafe array.  After all events are run,
   look through the array.  If promotion is set up correctly, then should get a
   positive number in array, then a negative number in array, then a positive
   number, etc.  (All positives should be strictly increasing; all negatives
   should be strictly decreasing.)
 */

public class PromotionTest
{
    // put results into this queue
    final static ConcurrentLinkedQueue<Double> tsafe_queue =
        new ConcurrentLinkedQueue<Double>();
    final static int NUM_THREADS_EACH_SIDE = 10;
    final static int NUM_EXTERNAL_CALLS = 100;
    final static AtomicBoolean had_exception = new AtomicBoolean(false);
    
    public static void main(String[] args)
    {
        if (PromotionTest.run_test())
            System.out.println("\nSUCCESS in PromotionTest\n");
        else
            System.out.println("\nFAILURE in PromotionTest\n");
    }

    public static ExecutorService create_executor()
    {
        ExecutorService executor = Executors.newFixedThreadPool(
            NUM_THREADS_EACH_SIDE,
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
        return executor;
    }
    
    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            PromoterEndpoint side_a = new PromoterEndpoint(
                new RalphGlobals(),"a_host_uuid",conn_obj);
            PromoterEndpoint side_b = new PromoterEndpoint(
                new RalphGlobals(),"b_host_uuid",conn_obj);

            ExecutorService executor_a = create_executor();
            ExecutorService executor_b = create_executor();

            //
            EndpointTask task_a = new EndpointTask(side_a,true);
            EndpointTask task_b = new EndpointTask(side_b,false);

            
            // put a bunch of tasks to increment a's number into
            // system.
            for (int i = 0; i < NUM_EXTERNAL_CALLS; ++i)
                executor_a.execute(task_a);

            // wait a bit so that those tasks can get good and
            // started.
            
            Thread.sleep(50);
            // put a bunch of tasks to increment b's number into
            // system.
            for (int i = 0; i < NUM_EXTERNAL_CALLS; ++i)
                executor_b.execute(task_b);
            

            // join on executor services
            executor_a.shutdown();
            executor_b.shutdown();
            while (!(executor_a.isTerminated() && executor_b.isTerminated()))
                Thread.sleep(50);
            
            // For now, just scan through queue to ensure ordering
            for (Double d : tsafe_queue)
                System.out.println(d);

            if (had_exception.get())
                return false;
            

        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        
        return true;
    }


    public static class EndpointTask implements Runnable
    {
        private PromoterEndpoint endpt_to_run_on = null;
        private boolean is_increment = false;

        public EndpointTask(
            PromoterEndpoint _endpt_to_run_on,boolean _is_increment)
        {
            is_increment = _is_increment;
            endpt_to_run_on = _endpt_to_run_on;

        }

        public void run()
        {
            try {
                Double result = endpt_to_run_on.call_from_external(is_increment);
                tsafe_queue.add(result);
            } catch(Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
        }
    }
    
}