package emit_test_harnesses;

import emit_test_package.StructLibUser.StructUser;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class StructLibraries
{
    public static void main(String[] args)
    {
        if (StructLibraries.run_test())
            System.out.println("\nSUCCESS in StructLibraries\n");
        else
            System.out.println("\nFAILURE in StructLibraries\n");
    }

    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            StructUser endpt = new StructUser(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            // testing numbers
            String text_to_set = "hello";
            endpt.set_struct_text(text_to_set);

            // should get out same text put in.
            if (! endpt.get_struct_text().equals(text_to_set))
                return false;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        
        return true;
    }
}