package emit_test_harnesses;

import ralph_emitted.SerializeNumberListJava.ListSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class SerializeNumberListTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeNumberListTest\n");
        else
            System.out.println("\nFAILURE in SerializeNumberListTest\n");
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

            // tests atomic number list serialization
            if (! num_list_sum_test(true,side_a))
                return false;
            // tests non atomic number list serialization
            if (! num_list_sum_test(false,side_a))
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static boolean num_list_sum_test(
        boolean atom,ListSerializer to_call_on)
        throws Exception
    {
        Double[] max_list_vals_to_sum =
            new Double [] {1.0, 2.0, 8.0, 20.0};

        for (int i = 0; i < max_list_vals_to_sum.length; ++i)
        {
            Double max_num = max_list_vals_to_sum[i];
            // the sum of integers from [0 to max_num) is
            // (max_num)(max_num-1)/2
            Double expected_value = max_num*(max_num -1)/2.0;

            Double result = null;
            if (atom)
                result = to_call_on.sum_atom_numbers(max_num);
            else
                result = to_call_on.sum_numbers(max_num);
            if (! result.equals(expected_value))
                return false;
        }
        return true;
    }
    
}