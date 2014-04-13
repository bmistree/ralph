package emit_test_harnesses;

import java.util.Random;

import ralph_emitted.InternalReturnStructJava.StructReturner;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class InternalStructMethodCallTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in InternalStructmethodCallTest\n");
        else
            System.out.println("\nFAILURE in InternalStructmethodCallTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            StructReturner service = new StructReturner(
                new RalphGlobals(),
                new SingleSideConnection());

            Random rand = new Random();
            for (int i = 0; i < 20; ++i)
            {
                Double a = rand.nextDouble();
                Double b = rand.nextDouble();
                Double expected_result = a + b;
                Double result = service.sum_numbers(a,b);
                if (! almost_equal(result,expected_result))
                    return false;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean almost_equal(Double a, Double b)
    {
        return Math.abs(a-b) < .00001;
    }
}
