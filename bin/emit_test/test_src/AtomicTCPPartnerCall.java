package emit_test_harnesses;

import emit_test_package.AtomicPartner.SideA;
import emit_test_package.AtomicPartner.SideB;
import RalphConnObj.TCPConnectionObj;
import ralph.RalphGlobals;
import ralph.EndpointConstructorObj;
import ralph.Endpoint;
import ralph.Ralph;
import java.io.IOException;

public class AtomicTCPPartnerCall
{
    private static final String HOST_NAME = "localhost";
    private static final int TCP_LISTENING_PORT = 39689;

    private static final SideAConstructor SIDE_A_CONSTRUCTOR =
        new SideAConstructor();
    private static final SideBConstructor SIDE_B_CONSTRUCTOR =
        new SideBConstructor();
    
    private static SideA side_a = null;
    private static SideB side_b = null;


    
    public static void main(String[] args)
    {
        if (AtomicTCPPartnerCall.run_test())
            System.out.println("\nSUCCESS in AtomicTCPPartnerCall\n");
        else
            System.out.println("\nFAILURE in AtomicTCPPartnerCall\n");
    }

    public static boolean run_test()
    {
        try
        {
            Ralph.tcp_accept(
                SIDE_B_CONSTRUCTOR, HOST_NAME, TCP_LISTENING_PORT);

            // wait for the other side to ensure that it's listening
            Thread.sleep(1000);
            try {
                side_a = (SideA)Ralph.tcp_connect(
                    SIDE_A_CONSTRUCTOR, HOST_NAME, TCP_LISTENING_PORT);
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
            RalphGlobals globals, String host_uuid,
            RalphConnObj.ConnectionObj conn_obj)
        {
            Endpoint to_return = null;

            try {
                to_return = new SideA(globals,host_uuid,conn_obj);
            } catch (Exception _ex) {
                _ex.printStackTrace();
                assert(false);
            }
            return to_return;
        }
    }
    
    private static class SideBConstructor implements EndpointConstructorObj
    {
        @Override
        public Endpoint construct(
            RalphGlobals globals, String host_uuid,
            RalphConnObj.ConnectionObj conn_obj)
        {
            try {
                side_b = new SideB(globals,host_uuid,conn_obj);
            } catch (Exception _ex) {
                _ex.printStackTrace();
            }
            return side_b;
        }
    }

    
}