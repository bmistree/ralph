package emit_test_harnesses;

import ralph_emitted.EnumJava.IEnumTest;
import ralph_emitted.EnumJava.Test;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class EnumTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in EnumTest\n");
        else
            System.out.println("\nFAILURE in EnumTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            Test endpt = new Test(
                new RalphGlobals(),new SingleSideConnection());
            return internal_test(endpt);
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    public static boolean internal_test(IEnumTest endpt) throws Exception
    {
        if (! endpt.test_monday_one().booleanValue())
            return false;
        
        if (! endpt.test_monday_two().booleanValue())
            return false;
        
        if (! endpt.test_not_monday().booleanValue())
            return false;
        
        return true;
    }
}