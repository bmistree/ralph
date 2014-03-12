package emit_test_harnesses;

import ralph_emitted.NestedStructTest.End;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.Arrays;
import java.util.ArrayList;


public class NestedStructs
{
    public static void main(String[] args)
    {
        if (NestedStructs.run_test())
            System.out.println("\nSUCCESS in NestedStructs\n");
        else
            System.out.println("\nFAILURE in NestedStructs\n");
    }

    public static boolean run_test()
    {
        try
        {
            End endpt = new End(
                new RalphGlobals(),new SingleSideConnection());

            ArrayList<Double> nums_to_insert = new ArrayList(
                Arrays.asList(
                    new Double(8),
                    new Double(1),
                    new Double(5),
                    new Double(9),
                    new Double(4),
                    new Double(3),
                    new Double(6),
                    new Double(20),
                    new Double(2),
                    new Double(10)));

            for (Double to_insert : nums_to_insert)
            {
                if (endpt.value_exists (to_insert).booleanValue())
                    return false;
                
                // first ensure that the value isn't already in the
                // binary search tree.
                endpt.insert_value(to_insert);
            }

            for (Double to_check : nums_to_insert)
            {
                if (! endpt.value_exists (to_check).booleanValue())
                    return false;
            }
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}