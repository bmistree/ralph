package emit_test_harnesses;

import ralph_emitted.BasicPartnerJava.SideA;
import ralph_emitted.BasicPartnerJava.SideB;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class BasicPartnerCall
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (BasicPartnerCall.run_test())
            System.out.println("\nSUCCESS in BasicPartnerCall\n");
        else
            System.out.println("\nFAILURE in BasicPartnerCall\n");
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
            SideA side_a = new SideA(
                new RalphGlobals(params_a),conn_obj);
            SideB side_b = new SideB(
                new RalphGlobals(params_b),conn_obj);

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
}