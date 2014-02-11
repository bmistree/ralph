package emit_test_harnesses;

import emit_test_package.BasicSpeculation.SpeculativeEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class BasicSpeculationTest
{
    public static void main(String[] args)
    {
        if (BasicSpeculationTest.run_test())
            System.out.println("\nSUCCESS in BasicSpeculationTest\n");
        else
            System.out.println("\nFAILURE in BasicSpeculationTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            SpeculativeEndpoint endpt = new SpeculativeEndpoint(
                new RalphGlobals(),
                new SingleSideConnection());

            // testing numbers
            double original_internal_number = endpt.get_number().doubleValue();

            // just check pipeline without speculation
            endpt.pipeline(13.0,false);
            endpt.pipeline(13.0,true);
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}