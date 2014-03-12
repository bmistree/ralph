package emit_test_harnesses;

import ralph_emitted.RangeTestJava.RangeTest;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class Range
{
    public static void main(String[] args)
    {
        if (Range.run_test())
            System.out.println("\nSUCCESS in Range\n");
        else
            System.out.println("\nFAILURE in Range\n");
    }

    public static double sum_double_array(double[] example_array)
    {
        double sum = 0;
        for (int i = 0; i < example_array.length; ++i)
            sum += example_array[i];
        return sum;
    }
    
    public static boolean run_test()
    {
        try
        {
            RangeTest rt_service = new RangeTest(
                new RalphGlobals(),new SingleSideConnection());

            // sum from [0, 10)
            double [] example_array =
                new double [] {0.,1.,2.,3.,4.,5.,6.,7.,8.,9.};
            double expected = sum_double_array(example_array);
            Double zero_to_ten = rt_service.sum_range(0.,10.,1.);
            if (! zero_to_ten.equals(expected))
                return false;
            
            // sum from [5,10)
            example_array = new double [] {5.,6.,7.,8.,9.};
            expected = sum_double_array(example_array);
            Double five_to_ten = rt_service.sum_range(5.,10.,1.);
            if (! five_to_ten.equals(expected))
                return false;
            
            // sum from [10,20), incrementing by 2
            example_array = new double [] {10.,12.,14.,16.,18.};
            expected = sum_double_array(example_array);
            Double ten_to_twenty = rt_service.sum_range(10.,20.,2.);
            if (! ten_to_twenty.equals(expected))
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