package emit_test_harnesses;

import ralph_emitted.LotsOfBackoutsJava.LotsOfBackouts;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class LotsOfBackoutsTest
{
    private final static int NUM_TIMES_TO_RUN = 10000;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in LotsOfBackoutsTest\n");
        else
            System.out.println("\nFAILURE in LotsOfBackoutsTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            LotsOfBackouts endpt = new LotsOfBackouts(
                new RalphGlobals(),
                new SingleSideConnection());

            endpt.perform_test(new Double(NUM_TIMES_TO_RUN));
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }
}