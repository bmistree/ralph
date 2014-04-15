package emit_test_harnesses;

import ralph_emitted.PartnerNumberReturnJava.PassReturnArg;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class PartnerCallReturn
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in PartnerCallReturn\n");
        else
            System.out.println("\nFAILURE in PartnerCallReturn\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            PassReturnArg side_a = new PassReturnArg(
                new RalphGlobals(TCP_CONNECTION_PORT_A),conn_obj);
            PassReturnArg side_b = new PassReturnArg(
                new RalphGlobals(TCP_CONNECTION_PORT_B),conn_obj);

            Double[] list_first_nums = new Double [] {1.0, 2.0, 3.5,5.5};
            Double[] list_second_nums = new Double [] {9.0, 2.2, 3.5,.5};

            for (int i = 0; i < list_first_nums.length; ++i)
            {
                Double num1 = list_first_nums[i];
                Double num2 = list_second_nums[i];
                            
                Double result = side_a.pass_sum_return(num1, num2);
                if (! result.equals(num1 + num2))
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