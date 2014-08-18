package emit_test_harnesses;

import ralph_emitted.EnumJava.IEnumTest;
import ralph_emitted.AliasEnumJava.Test;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class AliasEnumTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in AliasEnumTest\n");
        else
            System.out.println("\nFAILURE in AliasEnumTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            Test endpt = new Test(
                new RalphGlobals(),new SingleSideConnection());
            return EnumTest.internal_test(endpt);
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}