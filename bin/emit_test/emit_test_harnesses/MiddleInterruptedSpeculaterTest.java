package emit_test_harnesses;

import ralph_emitted.MiddleInterruptedSpeculaterJava.MiddleInterruptedSpeculater;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;



public class MiddleInterruptedSpeculaterTest
{
    final public static AtomicBoolean had_exception = new AtomicBoolean(false);
    final public static int NUM_TIMES_TO_RUN_TEST = 100;

    private final static RalphGlobals ralph_globals = new RalphGlobals();
    
    public static void main(String[] args)
    {
        if (MiddleInterruptedSpeculaterTest.run_test())
            System.out.println("\nSUCCESS in MiddleInterruptedSpeculaterTest\n");
        else
            System.out.println("\nFAILURE in MiddleInterruptedSpeculaterTest\n");
    }

    public static boolean run_test()
    {
        for (int i = 0; i < NUM_TIMES_TO_RUN_TEST; ++i)
        {
            if (! run_test_multiple())
                return false;
        }
        return true;
    }
    
    public static boolean run_test_multiple()
    {
        try
        {
            MiddleInterruptedSpeculater endpt = new MiddleInterruptedSpeculater(
                ralph_globals,
                new SingleSideConnection());

            run_concurrently(endpt);

            if (had_exception.get())
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static void run_concurrently(
        final MiddleInterruptedSpeculater endpt) throws Exception
    {
        Thread run_first = new MiddleInterruptedSpeculaterThread(
            endpt,MiddleInterruptedSpeculaterThread.WhichToRun.FIRST);
        Thread run_second = new MiddleInterruptedSpeculaterThread(
            endpt,MiddleInterruptedSpeculaterThread.WhichToRun.SECOND);
        Thread run_third = new MiddleInterruptedSpeculaterThread(
            endpt,MiddleInterruptedSpeculaterThread.WhichToRun.THIRD);
        
        run_first.start();
        Thread.sleep(10);
        run_second.start();
        Thread.sleep(10);
        run_third.start();

        run_first.join();
        run_second.join();
        run_third.join();
    }
    
    public static class MiddleInterruptedSpeculaterThread extends Thread
    {
        public enum WhichToRun
        {
            FIRST,SECOND,THIRD
        }

        private final MiddleInterruptedSpeculater endpt;
        private final WhichToRun which;
        
        public MiddleInterruptedSpeculaterThread(
            MiddleInterruptedSpeculater endpt,WhichToRun which)
        {
            this.endpt = endpt;
            this.which = which;
        }
        
        @Override
        public void run()
        {
            try
            {
                if (which == WhichToRun.FIRST)
                    endpt.run_first();
                else if (which == WhichToRun.SECOND)
                    endpt.run_second();
                else
                    endpt.run_third();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
        }
    }
}