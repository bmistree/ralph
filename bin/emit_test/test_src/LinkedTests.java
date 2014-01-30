package emit_test_harnesses;

import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import RalphConnObj.SameHostConnection;

import emit_test_package.LinkedInstance.LinkedInstanceEndpoint;
import emit_test_package.LinkedConnection.LinkedConnectionEndpoint;

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
    public static void main(String[] args)
    {
        if (LinkedTests.run_test())
            System.out.println("\nSUCCESS in LinkedTests\n");
        else
            System.out.println("\nFAILURE in LinkedTests\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ab_globals = new RalphGlobals();
            RalphGlobals cde_globals = new RalphGlobals();
            RalphGlobals fg_globals = new RalphGlobals();

            // a
            LinkedInstanceEndpoint a = new LinkedInstanceEndpoint(
                ab_globals,"", new SingleSideConnection());

            // connect b and c
            SameHostConnection bc_conn = new SameHostConnection();
            LinkedConnectionEndpoint b = new LinkedConnectionEndpoint(
                ab_globals,"",bc_conn);
            LinkedConnectionEndpoint c = new LinkedConnectionEndpoint(
                cde_globals,"",bc_conn);

            // d
            LinkedInstanceEndpoint d = new LinkedInstanceEndpoint(
                cde_globals,"", new SingleSideConnection());

            // connect e and f
            SameHostConnection ef_conn = new SameHostConnection();
            LinkedConnectionEndpoint e = new LinkedConnectionEndpoint(
                cde_globals,"",ef_conn);
            LinkedConnectionEndpoint f = new LinkedConnectionEndpoint(
                fg_globals,"",ef_conn);
            
            // create g
            LinkedInstanceEndpoint g = new LinkedInstanceEndpoint(
                fg_globals,"", new SingleSideConnection());

            
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