package emit_test_harnesses;

import ralph_emitted.InternalMethodCallJava.InternalMethodCaller;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class BasicInternalMethodCall
{
    public static void main(String[] args)
    {
        if (BasicInternalMethodCall.run_test())
            System.out.println("\nSUCCESS in BasicInternalMethodCall\n");
        else
            System.out.println("\nFAILURE in BasicInternalMethodCall\n");
    }

    public static boolean run_test()
    {
        try
        {
            InternalMethodCaller endpt = new InternalMethodCaller(
                new RalphGlobals(),
                new SingleSideConnection());

            // testing numbers
            double original_number = endpt.get_number().doubleValue();

            for (int i = 1; i <= 20; ++i)
            {
                endpt.increment_number();
                double new_number = endpt.get_number().doubleValue();
                if (new_number != (original_number + i))
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