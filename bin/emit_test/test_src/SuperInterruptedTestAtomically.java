package emit_test_harnesses;

import emit_test_package.SuperInterruptedAtomically.SuperInterruptedAtomic;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import ralph.EventPriority.IsSuperFlag;


public class SuperInterruptedTestAtomically
{
    final static int NUM_TIMES_TO_RUN = 5;
    private final static AtomicBoolean problem = new AtomicBoolean(false); 

    public enum ReturnCode
    {
        SUCCESS, FAILURE, UNKNOWN
    }
    
    public static void main(String[] args)
    {
        if (SuperInterruptedTestAtomically.run_test())
            System.out.println("\nSUCCESS in SuperInterruptedTestAtomically\n");
        else 
            System.out.println("\nFAILURE in SuperInterruptedTestAtomically\n");
    }

    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            SuperInterruptedAtomic endpt = new SuperInterruptedAtomic(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            for (int i=0; i < NUM_TIMES_TO_RUN; ++i)
                single_run(endpt);

            double final_value = endpt.get_num().doubleValue();

            if (final_value != (2.*NUM_TIMES_TO_RUN))
                return false;
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        boolean succeeded = ! problem.get();
        return succeeded;
    }

    private static void single_run(SuperInterruptedAtomic endpt)
    {
        ExecutorService executor_boosted = Executors.newSingleThreadExecutor();
        ExecutorService executor_super = Executors.newSingleThreadExecutor();
        LongEventer le_boosted = new LongEventer(endpt,false);
        LongEventer le_super = new LongEventer(endpt,true);

        executor_boosted.execute(le_boosted);
            
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            problem.set(true);
        }
        executor_super.execute(le_super);

        executor_boosted.shutdown();
        executor_super.shutdown();
        
        try {
            while (!(executor_super.isTerminated() && executor_boosted.isTerminated()))
                Thread.sleep(50);
        } catch (InterruptedException ex){}
    }


    
    public static class LongEventer implements Runnable
    {
        private SuperInterruptedAtomic endpt;
        private boolean run_as_super;
        public LongEventer(SuperInterruptedAtomic _endpt,boolean _run_as_super)
        {
            endpt = _endpt;
            run_as_super = _run_as_super;
        }

        public void run()
        {
            try
            {
                if (run_as_super)
                {
                    if (endpt.long_event(1.0,IsSuperFlag.SUPER).booleanValue())
                        problem.set(true);
                }
                else
                {
                    if (! endpt.long_event(1.0).booleanValue())
                        problem.set(true);
                }

            } catch (Exception ex)
            {
                ex.printStackTrace();
                problem.set(true);
            }
        }
    }
}