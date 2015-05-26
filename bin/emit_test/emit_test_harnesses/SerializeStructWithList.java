package emit_test_harnesses;

import ralph_emitted.SerializeStructWithListJava.ListSerializer;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.Ralph;

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

            // Instantiate ListSerializer and have it build a remote copy
            ListSerializer side_a = ListSerializer.external_create(globals_a);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    ListSerializer.factory, globals_a);
            side_a.install_partner(service_receiver_factory_to_send);


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