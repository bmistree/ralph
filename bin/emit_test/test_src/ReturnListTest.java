package emit_test_harnesses;

import emit_test_package.ReturnList.ListEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class ReturnListTest
{
    public static void main(String[] args)
    {
        if (ReturnListTest.run_test())
            System.out.println("\nSUCCESS in ReturnListTest\n");
        else
            System.out.println("\nFAILURE in ReturnListTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";
            
            ListEndpoint endpt = new ListEndpoint(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            endpt.test_return_list();
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}