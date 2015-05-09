package emit_test_harnesses;

import ralph_emitted.BasicPartnerJava.SideB;
import ralph_emitted.BasicSameHostPartnerJava.RemoteAccessor;

import ralph.RalphGlobals;
import ralph.Endpoint;
import ralph.Ralph;
import ralph.InternalServiceFactory;

public class TCPPartnerCall
{
    private static RemoteAccessor remote_accessor = null;
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;

    public static void main(String[] args)
    {
        if (TCPPartnerCall.run_test())
            System.out.println("\nSUCCESS in TCPPartnerCall\n");
        else
            System.out.println("\nFAILURE in TCPPartnerCall\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;
        RalphGlobals globals_a = new RalphGlobals(params_a);

        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;
        RalphGlobals globals_b = new RalphGlobals(params_b);

        try
        {
            // connect the two hosts together
            Ralph.tcp_connect(
                "127.0.0.1", TCP_CONNECTION_PORT_A, globals_b);

            InternalServiceFactory side_b_service_factory =
                new InternalServiceFactory(SideB.factory, globals_b);

            // wait for everything to settle down
            Thread.sleep(1000);

            // generate an endpoint which will then install a remote
            // service.
            RemoteAccessor remote_accessor =
                RemoteAccessor.create_single_sided(globals_a);

            remote_accessor.install_remote_endpt(side_b_service_factory);

            double init_number = 
                remote_accessor.get_remote_number().doubleValue();
            double expected_number = init_number;
            for (int i = 0; i < 20; ++i)
            {
                expected_number += i;
                remote_accessor.increment_other_side_number(new Double(i));
                double new_number =
                    remote_accessor.get_remote_number().doubleValue();

                if (expected_number != new_number)
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