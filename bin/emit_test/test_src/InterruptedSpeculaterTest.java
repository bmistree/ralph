package emit_test_harnesses;

import emit_test_package.InterruptedSpeculaterJava.InterruptedSpeculater;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicBoolean;



public class InterruptedSpeculaterTest
{
    final public static AtomicBoolean had_exception = new AtomicBoolean(false);
    
    public static void main(String[] args)
    {
        if (InterruptedSpeculaterTest.run_test())
            System.out.println("\nSUCCESS in InterruptedSpeculaterTest\n");
        else
            System.out.println("\nFAILURE in InterruptedSpeculaterTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            InterruptedSpeculater endpt = new InterruptedSpeculater(
                new RalphGlobals(),
                new SingleSideConnection());

            run_concurrently(endpt);
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static void run_concurrently(final InterruptedSpeculater endpt) throws Exception
    {
        // start evt1 and then an ms later, start evt2
        Thread t_evt1 = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    endpt.delayed_write();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    had_exception.set(true);
                }
            }
        };
        t_evt1.start();

        try
        {
            Thread.sleep(1);
        }
        catch (InterruptedException ex)
        {
            had_exception.set(true);
        }


        Thread t_evt2 = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    endpt.speculater();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                    had_exception.set(true);
                }
            }
        };
        t_evt2.start();
        t_evt1.join();
        t_evt2.join();

        try
        {
            endpt.simple_reads();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            had_exception.set(true);
        }
    }
}