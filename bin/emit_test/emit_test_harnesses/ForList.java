package emit_test_harnesses;

import ralph_emitted.ForListJava.ListEndpoint;
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
            ListEndpoint endpt =
                ListEndpoint.create_single_sided(new RalphGlobals());

            /** Check on non-atomic list */
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


            /** Check on atomic list */
            // append to end of atomic list
            sum_of_values_in_list = 0;
            for (int i = 5; i < 20; ++i)
            {
                Double to_insert = new Double((double)i);
                sum_of_values_in_list += to_insert;
                endpt.atomic_append_number(to_insert);
            }
            // calculate sum
            if (endpt.atomic_sum_list_numbers().doubleValue() != sum_of_values_in_list)
                return false;

            double value = endpt.break_list_numbers().doubleValue();
            if (value != 5.0)
                return false;

            value = endpt.continue_list_numbers().doubleValue();
            if (value != 0.0)
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