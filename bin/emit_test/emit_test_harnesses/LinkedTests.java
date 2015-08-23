package emit_test_harnesses;

import ralph.RalphGlobals;
import ralph.NonAtomicInternalList;
import ralph.InternalServiceFactory;
import ralph.Ralph;

import ralph_emitted.LinkedInstanceJava.LinkedInstanceEndpoint;

/**
   Here's topology:
   A->B->C
 */


public class LinkedTests
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    private final static int TCP_CONNECTION_PORT_C = 20496;

    public static void main(String[] args)
    {
        if (LinkedTests.run_test())
            System.out.println("\nSUCCESS in LinkedTests\n");
        else
            System.out.println("\nFAILURE in LinkedTests\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;

        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;

        RalphGlobals.Parameters params_c = new RalphGlobals.Parameters();
        params_c.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_C;

        try
        {
            RalphGlobals a_globals = new RalphGlobals(params_a);
            String a_uuid = a_globals.host_uuid;

            RalphGlobals b_globals = new RalphGlobals(params_b);
            String b_uuid = b_globals.host_uuid;

            RalphGlobals c_globals = new RalphGlobals(params_c);
            String c_uuid = c_globals.host_uuid;

            InternalServiceFactory linked_instance_factory =
                new InternalServiceFactory(
                    LinkedInstanceEndpoint.factory, a_globals);

            // generate connections between Ralph hosts
            connect(TCP_CONNECTION_PORT_A, b_globals);
            connect(TCP_CONNECTION_PORT_C, b_globals);

            // a
            LinkedInstanceEndpoint a =
                LinkedInstanceEndpoint.create_single_sided(a_globals);
            a.set_name("A");

            // generate a list of remote endpoint uuids
            NonAtomicInternalList<String,String> remote_uuids =
                a.get_empty_text_list();
            remote_uuids = a.append_text_to_text_list(b_uuid, remote_uuids);
            remote_uuids = a.append_text_to_text_list(c_uuid, remote_uuids);

            // generate a list of remote names
            NonAtomicInternalList<String,String> remote_names =
                a.get_empty_text_list();
            remote_names = a.append_text_to_text_list("B", remote_names);
            remote_names = a.append_text_to_text_list("C", remote_names);

            // do chained install
            a.chain_add_children(
                remote_uuids, remote_names, linked_instance_factory);

            // perform an increment on a.
            a.increment_and_request_others_to_increment();
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }

    public static void connect(
        int host_a_port_listening_on, RalphGlobals host_b_ralph_globals)
        throws Exception
    {
        Ralph.tcp_connect(
            "127.0.0.1", host_a_port_listening_on, host_b_ralph_globals);
    }
}