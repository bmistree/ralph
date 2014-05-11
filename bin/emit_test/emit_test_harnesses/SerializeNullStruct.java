package emit_test_harnesses;

import ralph_emitted.SerializeNullJava.SerializeNull;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;
import static emit_test_harnesses.SerializeStructHarnessHelper.num_sum_test;

public class SerializeNullStruct
{
    // Using inexact check for floating point equality between doubles
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeNullStruct\n");
        else
            System.out.println("\nFAILURE in SerializeNullStruct\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            SerializeNull side_a = new SerializeNull(
                new RalphGlobals(TCP_CONNECTION_PORT_A),conn_obj);
            SerializeNull side_b = new SerializeNull(
                new RalphGlobals(TCP_CONNECTION_PORT_B),conn_obj);

            if (! num_sum_test(side_a))
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