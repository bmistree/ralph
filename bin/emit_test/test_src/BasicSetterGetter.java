package emit_test_harnesses;

import emit_test_package.BasicRalph.SetterGetter;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class BasicSetterGetter
{
    public static void main(String[] args)
    {
        if (BasicSetterGetter.run_test())
            System.out.println("\nSUCCESS in BasicSetterGetter\n");
        else
            System.out.println("\nFAILURE in BasicSetterGetter\n");
    }

    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            SetterGetter endpt = new SetterGetter(
                        new RalphGlobals(),
                        dummy_host_uuid,
                        new SingleSideConnection());

            double original_internal_number = endpt.get_number().doubleValue();
            double new_number = original_internal_number + 1;
            endpt.set_number(new_number);
            double gotten_number = endpt.get_number().doubleValue();
            if (gotten_number != new_number)
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