package emit_test_harnesses;

import emit_test_package.InternalCreateServiceJava.InternalCreateService;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class InternalCreateServiceTest
{
    public static void main(String[] args)
    {
        if (InternalCreateServiceTest.run_test())
            System.out.println("\nSUCCESS in InternalCreateServiceTest\n");
        else
            System.out.println("\nFAILURE in InternalCreateServiceTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            InternalCreateService endpt = new InternalCreateService(
                new RalphGlobals(),
                new SingleSideConnection());

            if (! endpt.is_null())
                return false;

            endpt.setup_service();
            if (endpt.is_null())
                return false;

            double internal_number = endpt.get_number().doubleValue();
            double new_internal_number = internal_number + 1;
            endpt.set_number(new_internal_number);

            if (! endpt.get_number().equals(new_internal_number))
                return false;

            endpt.setup_service();
            if (!endpt.get_number().equals(internal_number))
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