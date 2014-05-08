package emit_test_harnesses;

import java.lang.Math;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import ralph_emitted.ISerializeStructSumJava.ISerializeStructSum;


public class SerializeStructHarnessHelper
{
    private final static double THRESHOLD_DELTA_BETWEEN_DOUBLES = .000001;
    private final static Random rand = new Random();
    
    public static boolean num_sum_test(ISerializeStructSum to_call_on)
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
            result = to_call_on.sum_numbers(a,b,c,d);
            
            if (! threshold_equal(result,expected_value))
                return false;
        }
        return true;
    }

    public static boolean threshold_equal(double a, double b)
    {
        return Math.abs(a-b) < THRESHOLD_DELTA_BETWEEN_DOUBLES;
    }

}

