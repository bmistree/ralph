package emit_test_harnesses;


import ralph_emitted.SerializeStructWithListJava.ListSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

import static emit_test_harnesses.SerializeStructHarnessHelper.num_sum_test;

public class SerializeStructWithList
{
    // Using inexact check for floating point equality between doubles
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeStructWithList\n");
        else
            System.out.println("\nFAILURE in SerializeStructWithList\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            ListSerializer side_a = new ListSerializer(
                new RalphGlobals(TCP_CONNECTION_PORT_A),conn_obj);
            ListSerializer side_b = new ListSerializer(
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