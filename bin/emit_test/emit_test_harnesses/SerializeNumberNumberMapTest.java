package emit_test_harnesses;

import ralph_emitted.SerializeNumMapJava.MapSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class SerializeNumberNumberMapTest
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeNumberNumberMapTest\n");
        else
            System.out.println("\nFAILURE in SerializeNumberNumberMapTest\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;
        
        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;

        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            MapSerializer side_a = new MapSerializer(
                new RalphGlobals(params_a),conn_obj);
            MapSerializer side_b = new MapSerializer(
                new RalphGlobals(params_b),conn_obj);

            // tests atomic number list serialization
            if (! num_map_sum_test(true,side_a))
                return false;
            // tests non atomic number list serialization
            if (! num_map_sum_test(false,side_a))
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    public static boolean num_map_sum_test(
        boolean atom,MapSerializer to_call_on)
        throws Exception
    {
        Double[] max_list_vals_to_sum =
            new Double [] {1.0, 2.0, 8.0, 20.0};

        for (int i = 0; i < max_list_vals_to_sum.length; ++i)
        {
            Double max_num = max_list_vals_to_sum[i];
            Boolean result = null;
            if (atom)
                result = to_call_on.atom_sum_numbers(max_num);
            else
                result = to_call_on.sum_numbers(max_num);
            if (! result.booleanValue())
                return false;
        }
        return true;
    }
}