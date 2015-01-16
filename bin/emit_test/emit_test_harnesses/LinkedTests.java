package emit_test_harnesses;

import ralph.RalphGlobals;
import RalphConnObj.SameHostConnection;

import ralph_emitted.LinkedInstanceJava.LinkedInstanceEndpoint;
import ralph_emitted.LinkedConnectionJava.LinkedConnectionEndpoint;

/**
   Here's topology:

   A->B
       \
        C->D->E
               \
                F->G

   Where A, D, and G are LinkedInstanceEndpoints;
   B, C, E, and F are LinkedInstanceConnections.

   And A,B are on the same host; C,D,E are on the same host; F,G are
   on the same host.

   Arrows indicate who holds references to whom.
 */


public class LinkedTests
{
    private final static int TCP_CONNECTION_PORT_AB = 20494;
    private final static int TCP_CONNECTION_PORT_CDE = 20495;
    private final static int TCP_CONNECTION_PORT_FG = 20496;
    
    public static void main(String[] args)
    {
        if (LinkedTests.run_test())
            System.out.println("\nSUCCESS in LinkedTests\n");
        else
            System.out.println("\nFAILURE in LinkedTests\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_ab = new RalphGlobals.Parameters();
        params_ab.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_AB;
        
        RalphGlobals.Parameters params_cde = new RalphGlobals.Parameters();
        params_cde.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_CDE;
        
        RalphGlobals.Parameters params_fg = new RalphGlobals.Parameters();
        params_fg.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_FG;
        
        try
        {
            RalphGlobals ab_globals = new RalphGlobals(params_ab);
            RalphGlobals cde_globals = new RalphGlobals(params_cde);
            RalphGlobals fg_globals = new RalphGlobals(params_fg);

            // a
            LinkedInstanceEndpoint a =
                LinkedInstanceEndpoint.create_single_sided(ab_globals);
            a.set_name("A");

            // connect b and c
            SameHostConnection bc_conn = new SameHostConnection();
            LinkedConnectionEndpoint b =
                LinkedConnectionEndpoint.external_create(
                    ab_globals,bc_conn);
            LinkedConnectionEndpoint c =
                LinkedConnectionEndpoint.external_create(
                    cde_globals,bc_conn);
            b.set_name("B");
            c.set_name("C");
            
            // d
            LinkedInstanceEndpoint d =
                LinkedInstanceEndpoint.create_single_sided(cde_globals);
            d.set_name("D");
            
            // connect e and f
            SameHostConnection ef_conn = new SameHostConnection();
            LinkedConnectionEndpoint e =
                LinkedConnectionEndpoint.external_create(cde_globals,ef_conn);
            LinkedConnectionEndpoint f =
                LinkedConnectionEndpoint.external_create(fg_globals,ef_conn);
            e.set_name("E");
            f.set_name("F");

            // create g
            LinkedInstanceEndpoint g =
                LinkedInstanceEndpoint.create_single_sided(fg_globals);
            g.set_name("G");
            
            
            // add directional connections

            // let a know of b
            a.add_child(b);

            // let c know of d
            c.set_instance_endpoint(d);

            // let d know of e
            d.add_child(e);

            // let f know of g
            f.set_instance_endpoint(g);

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
}