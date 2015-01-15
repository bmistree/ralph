package emit_test_harnesses;

import ralph_emitted.SelfJava.B;
import ralph.RalphGlobals;

public class SelfTest
{
    public static void main(String[] args)
    {
        if (SelfTest.run_test())
            System.out.println("\nSUCCESS in SelfTest\n");
        else
            System.out.println("\nFAILURE in SelfTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            B endpt = B.create_single_sided(new RalphGlobals());

            for (int i = 1; i < 20; ++i)
            {
                Double result = endpt.create_and_increment();
                if (! result.equals(new Double(i)))
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