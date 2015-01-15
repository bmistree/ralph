package emit_test_harnesses;

import ralph_emitted.ForMapJava.MapEndpoint;
import ralph.RalphGlobals;

public class ForMap
{
    public static void main(String[] args)
    {
        if (ForMap.run_test())
            System.out.println("\nSUCCESS in ForMap\n");
        else
            System.out.println("\nFAILURE in ForMap\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            MapEndpoint endpt =
                MapEndpoint.create_single_sided(new RalphGlobals());

            /** Check on non-atomic map */
            // insert number in map
            double sum_of_values_in_map = 0;
            for (int i = 5; i < 20; ++i)
            {
                Double to_insert = new Double((double)i);
                Double to_insert_index = new Double((double)i+30);
                sum_of_values_in_map += to_insert;
                endpt.insert_number(to_insert_index,to_insert);
            }
            // calculate sum
            if (endpt.sum_map_numbers().doubleValue() != sum_of_values_in_map)
                return false;

            /** Check on atomic map */
            // insert number in map
            sum_of_values_in_map = 0;
            for (int i = 5; i < 20; ++i)
            {
                Double to_insert = new Double((double)i);
                Double to_insert_index = new Double((double)i+30);
                sum_of_values_in_map += to_insert;
                endpt.atomic_insert_number(to_insert_index,to_insert);
            }
            // calculate sum
            if (endpt.atomic_sum_map_numbers().doubleValue() != sum_of_values_in_map)
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