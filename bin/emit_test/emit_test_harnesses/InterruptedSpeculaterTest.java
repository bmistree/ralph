package emit_test_harnesses;

import ralph_emitted.InterruptedSpeculaterJava.InterruptedSpeculater;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;



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
            InterruptedSpeculater endpt =
                InterruptedSpeculater.create_single_sided( new RalphGlobals());

            if (run_concurrently(endpt,false) < 2)
                return false;

            if (run_concurrently(endpt,true) != 1)
                return false;

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


    public static int run_concurrently(
        final InterruptedSpeculater endpt,final boolean read_speculater) throws Exception
    {
        final AtomicInteger int_holder = new AtomicInteger(0);

        // start evt1 and then an ms later, start evt2
        Thread t_evt1 = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    int_holder.set(
                        (int)endpt.delayed_write().doubleValue());
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
                    if (read_speculater)
                        endpt.read_speculater();
                    else
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

        return int_holder.get();
    }
}