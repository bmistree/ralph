package emit_test_harnesses;

import ralph_emitted.PromotionJava.PromoterEndpoint;
import ralph.RalphGlobals;
import ralph.Endpoint;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Math;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;

/**
   Reproduced from top of promotion.rph file.

   Want to ensure that wound-wait deadlock avoidance works as
   anticipated.

*/
public class WoundWaitTest
{
    // put results into this queue
    final static ConcurrentLinkedQueue<Double> tsafe_queue =
        new ConcurrentLinkedQueue<Double>();
    final static int NUM_THREADS_EACH_SIDE = 40;
    final static int NUM_EXTERNAL_CALLS = 40;
    final static AtomicBoolean had_exception = new AtomicBoolean(false);
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (WoundWaitTest.run_test())
            System.out.println("\nSUCCESS in WoundWaitTest\n");
        else
            System.out.println("\nFAILURE in WoundWaitTest\n");
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
        params_a.deadlock_avoidance_algorithm = DeadlockAvoidanceAlgorithm.WOUND_WAIT;
        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;
        params_b.deadlock_avoidance_algorithm = DeadlockAvoidanceAlgorithm.WOUND_WAIT;
        
        try
        {
            PromoterEndpoint side_a = PromoterEndpoint.external_create(
                new RalphGlobals(params_a));

            PromoterEndpoint side_b = PromoterEndpoint.external_create(
                new RalphGlobals(params_b));

            side_a.set_promoter(side_b);
            side_b.set_promoter(side_a);

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
            
            Thread.sleep(150);
            // put a bunch of tasks rooted at B into system.
            for (int i = 0; i < NUM_EXTERNAL_CALLS; ++i)
                executor_b.execute(task_b);
            

            // join on executor services
            executor_a.shutdown();
            executor_b.shutdown();
            while (!(executor_a.isTerminated() && executor_b.isTerminated()))
                Thread.sleep(50);

            // scan through array: all of one endpoint's events should
            // execute before the other's.
            for (int i =0; i < NUM_EXTERNAL_CALLS; ++i)
            {
                Double d = tsafe_queue.poll();
                if (! d.equals(1.0))
                    return false;
            }
            for (int i =0; i < NUM_EXTERNAL_CALLS; ++i)
            {
                Double d = tsafe_queue.poll();
                if (! d.equals(-1.0))
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