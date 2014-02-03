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
            TestAtomically endpt = new TestAtomically(
                new RalphGlobals(),
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

            double to_set_to = 6;
            endpt.nested_atomically(to_set_to - 3.);
            if (endpt.get_number().doubleValue() != to_set_to)
                return false;
            
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }
}