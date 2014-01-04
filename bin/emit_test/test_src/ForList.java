package emit_test_harnesses;

import emit_test_package.ForListTest.ListEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class ForList
{
    public static void main(String[] args)
    {
        if (ForList.run_test())
            System.out.println("\nSUCCESS in ForList\n");
        else
            System.out.println("\nFAILURE in ForList\n");
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

            // append to end of list
            double sum_of_values_in_list = 0;
            for (int i = 5; i < 20; ++i)
            {
                Double to_insert = new Double((double)i);
                sum_of_values_in_list += to_insert;
                endpt.append_number(to_insert);
            }
            // calculate sum
            if (endpt.sum_list_numbers().doubleValue() != sum_of_values_in_list)
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