package emit_test_harnesses;

import ralph_emitted.EnumStructJava.UseBool;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class StructEnumTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in StructEnumTest\n");
        else
            System.out.println("\nFAILURE in StructEnumTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            UseBool endpt = new UseBool(
                new RalphGlobals(),new SingleSideConnection());

            if (! endpt.test_true_false(true))
                return false;

            if (endpt.test_true_false(false))
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}