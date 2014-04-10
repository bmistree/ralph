package emit_test_harnesses;

import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import ralph_emitted.SerializeMapOfStructsJava.MapSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class SerializeMapOfStructs
{
    private final static Random rand = new Random();
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeMapOfStructs\n");
        else
            System.out.println("\nFAILURE in SerializeMapOfStructs\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            MapSerializer side_a = new MapSerializer(
                new RalphGlobals(),conn_obj);
            MapSerializer side_b = new MapSerializer(
                new RalphGlobals(),conn_obj);

            // tests atomic number map serialization
            if (! map_num_sum_test(true,side_a))
                return false;
            // tests non atomic number map serialization
            if (! map_num_sum_test(false,side_a))
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static boolean map_num_sum_test(
        boolean atom,MapSerializer to_call_on)
        throws Exception
    {
        List<Double[]> num_vals_to_sum = new ArrayList<Double[]>(
            Arrays.asList(
                new Double [] {1.0, 2.0, 18.0, 20.0},
                new Double [] {-1.0, 2.0, 8.0, 20.0},
                new Double [] {1.0, 30.0, 8.0, 20.0},
                new Double [] {1.0, 2.0, 8.0, 2.0},
                new Double [] {1.0, 2.0, -8.0, 2.0},
                new Double [] {1.0, 2.3, 8.5, 20.0},
                new Double [] {1.0, .4, 8.0, 20.0}));

        for (int i = 0; i < 20; ++i)
        {
            num_vals_to_sum.add(
                new Double [] {
                    rand.nextDouble(),
                    rand.nextDouble(),
                    rand.nextDouble(),
                    rand.nextDouble()});
        }
        

        for (int i = 0; i < num_vals_to_sum.size(); ++i)
        {
            Double [] four_tuple = num_vals_to_sum.get(i);
            Double a = four_tuple[0];
            Double b = four_tuple[1];
            Double c = four_tuple[2];
            Double d = four_tuple[3];
            Double expected_value = a+b+c+d;

            Double result = null;
            if (atom)
                result = to_call_on.atom_sum_numbers(a,b,c,d);
            else
                result = to_call_on.sum_numbers(a,b,c,d);

            if (! result.equals(expected_value))
                return false;
        }
        return true;
    }
}