package emit_test_harnesses;

import emit_test_package.Null.NullService;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class NullTest
{
    public static void main(String[] args)
    {
        if (NullTest.run_test())
            System.out.println("\nSUCCESS in NullTest\n");
        else
            System.out.println("\nFAILURE in NullTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            NullService service = new NullService(
                new RalphGlobals(),
                new SingleSideConnection());

            if (service.get_number() != null)
                return false;

            if (service.get_text() != null)
                return false;

            if (service.get_tf() != null)
                return false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
