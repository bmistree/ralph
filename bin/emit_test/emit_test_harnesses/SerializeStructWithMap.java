package emit_test_harnesses;

import ralph_emitted.SerializeStructWithMapJava.MapSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;
import static emit_test_harnesses.SerializeStructHarnessHelper.num_sum_test;

public class SerializeStructWithMap
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeStructWithMap\n");
        else
            System.out.println("\nFAILURE in SerializeStructWithMap\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            MapSerializer side_a = new MapSerializer(
                new RalphGlobals(TCP_CONNECTION_PORT_A),conn_obj);
            MapSerializer side_b = new MapSerializer(
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