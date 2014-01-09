package emit_test_harnesses;

import emit_test_package.RalphAtomically.TestAtomically;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class BasicTestAtomically
{
    public static void main(String[] args)
    {
        if (BasicTestAtomically.run_test())
            System.out.println("\nSUCCESS in BasicTestAtomically\n");
        else
            System.out.println("\nFAILURE in BasicTestAtomically\n");
    }

    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            TestAtomically endpt = new TestAtomically(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            for (int i = 0; i < 30; ++i)
            {
                double original_number = endpt.get_number().doubleValue();
                endpt.recursive_increment_number(new Double((double)i));
                double current_number = endpt.get_number().doubleValue();

                if (current_number != (original_number + i))
                    return false;
            }

            // check that can use two atomic statements in same
            // ActiveEvent.
            endpt.double_atomically();
            endpt.double_atomically();

            endpt.same_method_double_atomically();
            endpt.same_method_double_atomically();

        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }
}