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

            Double some_double = new Double(3);
            result = endpt.convert(some_double);
            if (! result.equals(some_double.toString()))
                return false;


            Double some_double2 = new Double (4);
            Double some_double3 = new Double (5);

            result = endpt.many_add_and_convert(
                some_double,lhs,some_double2,some_double3,
                rhs);

            String expected =
                some_double.toString() + lhs + some_double2.toString() +
                some_double3.toString() + rhs;
            
            if (! expected.equals(result))
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