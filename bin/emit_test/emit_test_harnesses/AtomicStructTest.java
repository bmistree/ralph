package emit_test_harnesses;

import ralph_emitted.AtomicStructJava.TVarStructTest;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicStructTest
{
    private final static AtomicBoolean had_exception = new AtomicBoolean(false);;
    private final static int NUM_TIMES_TO_RUN_LONG_EVENT = 50;
    private final static int NUM_SHORT_EVENTS_PER_LONG_EVENT = 50;

    
    public static void main(String[] args)
    {
        if (AtomicStructTest.run_test())
            System.out.println("\nSUCCESS in AtomicStructTest\n");
        else
            System.out.println("\nFAILURE in AtomicStructTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            TVarStructTest endpt = new TVarStructTest(
                new RalphGlobals(),
                new SingleSideConnection());

            for (int i = 0; i < NUM_TIMES_TO_RUN_LONG_EVENT; ++i)
            {
                if (!run_single_test(endpt))
                    return false;

                if (had_exception.get())
                    return false;
            }
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }

    private static class LongEventThread extends Thread
    {
        private final TVarStructTest endpt;
        public int result = -1;

        public LongEventThread(TVarStructTest _endpt)
        {
            endpt = _endpt;
        }
        @Override
        public void run()
        {
            try
            {
                Double ralph_result = endpt.long_event();
                result = ralph_result.intValue();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
        }
    }

    private static class ShortEventThread extends Thread
    {
        private final TVarStructTest endpt;
        public ShortEventThread(TVarStructTest _endpt)
        {
            endpt = _endpt;
        }
        @Override
        public void run()
        {
            try
            {
                for (int i = 0; i < NUM_SHORT_EVENTS_PER_LONG_EVENT; ++i)
                    endpt.short_event();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
            }
        }        
    }

    private static boolean run_single_test(TVarStructTest endpt)
    {
        // start running the long event
        LongEventThread let = new LongEventThread(endpt);
        ShortEventThread set = new ShortEventThread(endpt);
        let.start();
        set.start();
        try
        {
            let.join();
            set.join();
        }
        catch(InterruptedException ex)
        {
            ex.printStackTrace();
            had_exception.set(true);
        }

        if (let.result != 0)
            return false;
        return true;
    }
}
    
