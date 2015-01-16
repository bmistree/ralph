package emit_test_harnesses;

import ralph_emitted.PromotionJava.PromoterEndpoint;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;
import ralph.Endpoint;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Math;

/**
   Reproduced from top of promotion.rph file.
   
   Want to ensure that get quality of service amongst endpoints.  Way to test
   this is to create two, connected LongAndWrite endpoints, A and B.  Issue a
   bunch of call_from_external calls on A (with arg true).  Before these
   complete, issue a bunch of call_from_external calls on B (with arg false).

   This means that we'll cause a bunch on both A and B.

   Put results of each call into a threadsafe array.  After all events are run,
   look through the array.  If promotion is set up correctly, then should get a
   1 in array, then a -1 in array, then 1, etc.

   Note: there could occasionally be some out-of-orderness, as we can't
   guarantee that the result of one transaction gets put into the threadsafe
   queue before other transactions that completed after it.  But the timeouts
   that we've selected seem as though they may reasonably ensure ordering.

*/
public class PromotionTest
{
    // put results into this queue
    final static ConcurrentLinkedQueue<Double> tsafe_queue =
        new ConcurrentLinkedQueue<Double>();
    final static int NUM_THREADS_EACH_SIDE = 10;
    final static int NUM_EXTERNAL_CALLS = 40;
    final static AtomicBoolean had_exception = new AtomicBoolean(false);
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;

    
    public static void main(String[] args)
    {
        if (run_test())
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
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;
        
        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;

        
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            PromoterEndpoint side_a =
                PromoterEndpoint.external_create(
                    new RalphGlobals(params_a),conn_obj);
            PromoterEndpoint side_b =
                PromoterEndpoint.external_create(
                    new RalphGlobals(params_b),conn_obj);

            ExecutorService executor_a = create_executor();
            ExecutorService executor_b = create_executor();

            //
            EndpointTask task_a = new EndpointTask(side_a,true);
            EndpointTask task_b = new EndpointTask(side_b,false);

            
            // put a bunch of tasks rooted at A into system.
            for (int i = 0; i < NUM_EXTERNAL_CALLS; ++i)
                executor_a.execute(task_a);

            // wait a bit so that those tasks can get good and
            // started.
            
            Thread.sleep(50);
            // put a bunch of tasks rooted at B into system.
            for (int i = 0; i < NUM_EXTERNAL_CALLS; ++i)
                executor_b.execute(task_b);
            

            // join on executor services
            executor_a.shutdown();
            executor_b.shutdown();
            while (!(executor_a.isTerminated() && executor_b.isTerminated()))
                Thread.sleep(50);
            
            // Scan through array: events should alternate.  This means that
            // should see 1, then -1, then 1, then -1, etc.: a partial sum of
            // the array should never yield a result where abs of the sum > 1.
            int running_sum = 0;
            for (Double d : tsafe_queue)
            {
                running_sum += d.intValue();
                if (Math.abs(running_sum) > 1)
                    return false;
            }

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
        private boolean is_positive = false;

        public EndpointTask(
            PromoterEndpoint _endpt_to_run_on,boolean _is_positive)
        {
            is_positive = _is_positive;
            endpt_to_run_on = _endpt_to_run_on;
        }

        public void run()
        {
            try {
                Double result =
                    endpt_to_run_on.call_from_external(is_positive);
                tsafe_queue.add(result);
            } catch(Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
        }
    }
    
}