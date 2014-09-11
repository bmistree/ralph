package emit_test_harnesses;

import java.util.List;

import ralph_emitted.BasicPartnerJava.SideA;
import ralph_emitted.BasicPartnerJava.SideB;
import RalphConnObj.TCPConnectionObj;
import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.EndpointConstructorObj;
import ralph.Endpoint;
import ralph.Ralph;
import java.io.IOException;

public class TCPPartnerCall
{
    private static final String HOST_NAME = "localhost";
    private static final int TCP_LISTENING_PORT = 38689;

    private static final SideAConstructor SIDE_A_CONSTRUCTOR =
        new SideAConstructor();
    private static final SideBConstructor SIDE_B_CONSTRUCTOR =
        new SideBConstructor();
    
    private static SideA side_a = null;
    private static SideB side_b = null;
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
        
        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;
        
        try
        {
            Ralph.tcp_accept(
                SIDE_B_CONSTRUCTOR, HOST_NAME, TCP_LISTENING_PORT,
                new RalphGlobals(params_b));

            // wait for the other side to ensure that it's listening
            Thread.sleep(1000);
            try {
                side_a = (SideA)Ralph.tcp_connect(
                    SIDE_A_CONSTRUCTOR, HOST_NAME, TCP_LISTENING_PORT,
                    new RalphGlobals(params_a));
            } catch (IOException e) {
                e.printStackTrace();
                assert(false);
            }

            // wait for everything to settle down
            Thread.sleep(1000);
            double prev_number = side_b.get_number().doubleValue();

            for (int i = 0; i < 20; ++i)
            {
                side_a.increment_other_side_number(new Double(i));
                double new_number = side_b.get_number().doubleValue();

                if ( (prev_number + i) != new_number)
                    return false;
                
                prev_number = new_number;
            }
                        
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    private static class SideAConstructor implements EndpointConstructorObj
    {
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj)
        {
            Endpoint to_return = null;

            try {
                to_return = new SideA(globals,conn_obj);
            } catch (Exception _ex) {
                _ex.printStackTrace();
                assert(false);
            }
            return to_return;
        }
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj,
            List<RalphObject> internal_val_list)
        {
            return construct(globals,conn_obj);
        }
    }
    
    private static class SideBConstructor implements EndpointConstructorObj
    {
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj)
        {
            try {
                side_b = new SideB(globals,conn_obj);
            } catch (Exception _ex) {
                _ex.printStackTrace();
            }
            return side_b;
        }
        @Override
        public Endpoint construct(
            RalphGlobals globals, RalphConnObj.ConnectionObj conn_obj,
            List<RalphObject> internal_val_list)
        {
            return construct(globals,conn_obj);
        }
    }
}