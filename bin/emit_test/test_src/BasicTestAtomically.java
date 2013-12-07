package emit_test_harnesses;

import emit_test_package.Ralph.TestAtomically;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class BasicTestAtomically
{
    public static void main(String[] args)
    {
        if (BasicSetterGetter.run_test())
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

                endpt.recursive_increment_number(i);

                double current_number = endpt.get_number().doubleValue();

                if (current_number != (original_number + i))
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
}