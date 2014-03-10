package emit_test_harnesses;

import emit_test_package.ReadReadWriteSpeculationJava.ReadReadWrite;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicBoolean;



public class ReadReadWriteTest
{
    final public static AtomicBoolean had_exception = new AtomicBoolean(false);
    
    public static void main(String[] args)
    {
        if (ReadReadWriteTest.run_test())
            System.out.println("\nSUCCESS in ReadReadWriteTest\n");
        else
            System.out.println("\nFAILURE in ReadReadWriteTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            ReadReadWrite endpt = new ReadReadWrite(
                new RalphGlobals(),
                new SingleSideConnection());

            // testing numbers
            double original_internal_number = endpt.get_num().doubleValue();
            long start = System.currentTimeMillis();
            run_concurrently(endpt);
            long end = System.currentTimeMillis();
            long time_in_ms = end-start;
            
            if (had_exception.get())
                return false;
                
            double now_internal_number = endpt.get_num().doubleValue();            
            if (now_internal_number != (original_internal_number + 1))
                return false;

            // test timing
            if (time_in_ms > 300)
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static void run_concurrently(final ReadReadWrite endpt) throws Exception
    {
        // start evt1 and then an ms later, start evt2
        Thread t_evt1 = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    endpt.evt1();
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
                    endpt.evt2();
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
    }

    
}