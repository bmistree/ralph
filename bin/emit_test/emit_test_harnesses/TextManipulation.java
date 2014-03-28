package emit_test_harnesses;

import ralph_emitted.TextAddAndToTextJava.TextAdder;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class TextManipulation
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in TextManipulation\n");
        else
            System.out.println("\nFAILURE in TextManipulation\n");
    }

    public static boolean run_test()
    {
        try
        {
            TextAdder endpt = new TextAdder(
                new RalphGlobals(),
                new SingleSideConnection());

            String lhs = "hello";
            String rhs = "hi";
            String result = endpt.return_addition(lhs,rhs);

            if (! result.equals(lhs + rhs))
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