package emit_test_harnesses;

import ralph_emitted.SerializeNumberListJava.ListSerializer;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.Ralph;

public class SerializeNumberListTest
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;

    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeNumberListTest\n");
        else
            System.out.println("\nFAILURE in SerializeNumberListTest\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;

        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;


        try
        {
            RalphGlobals globals_a = new RalphGlobals(params_a);
            RalphGlobals globals_b = new RalphGlobals(params_b);

            // connect hosts a and b, via a tcp connection
            Thread.sleep(500);
            Ralph.tcp_connect("127.0.0.1", TCP_CONNECTION_PORT_B, globals_a);
            Thread.sleep(500);


            ListSerializer side_a = ListSerializer.external_create(globals_a);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    ListSerializer.factory, globals_a);
            side_a.install_partner(service_receiver_factory_to_send);

            // tests atomic number list serialization
            if (! num_list_sum_test(true,side_a))
                return false;
            // tests non atomic number list serialization
            if (! num_list_sum_test(false,side_a))
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static boolean num_list_sum_test(
        boolean atom,ListSerializer to_call_on)
        throws Exception
    {
        Double[] max_list_vals_to_sum =
            new Double [] {1.0, 2.0, 8.0, 20.0};

        for (int i = 0; i < max_list_vals_to_sum.length; ++i)
        {
            Double max_num = max_list_vals_to_sum[i];
            // the sum of integers from [0 to max_num) is
            // (max_num)(max_num-1)/2
            Double expected_value = max_num*(max_num -1)/2.0;

            Double result = null;
            if (atom)
                result = to_call_on.sum_atom_numbers(max_num);
            else
                result = to_call_on.sum_numbers(max_num);
            if (! result.equals(expected_value))
                return false;
        }
        return true;
    }

}