package emit_test_harnesses;

import ralph_emitted.SerializeListJava.ListSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class SerializeListTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeListTest\n");
        else
            System.out.println("\nFAILURE in SerializeListTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            ListSerializer side_a = new ListSerializer(
                new RalphGlobals(),conn_obj);
            ListSerializer side_b = new ListSerializer(
                new RalphGlobals(),conn_obj);

            Double[] max_list_vals_to_sum =
                new Double [] {1.0, 2.0, 8.0, 20.0};

            for (int i = 0; i < max_list_vals_to_sum.length; ++i)
            {
                Double max_num = max_list_vals_to_sum[i];
                // the sum of integers from [0 to max_num) is
                // (max_num)(max_num-1)/2
                Double expected_value = max_num*(max_num -1)/2.0;
                            
                Double result = side_a.sum_numbers(max_num);
                if (! result.equals(expected_value))
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