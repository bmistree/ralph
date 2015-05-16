package emit_test_harnesses;

import ralph_emitted.AtomicPartnerJava.SideA;
import ralph_emitted.AtomicPartnerJava.SideB;
import ralph.RalphGlobals;

public class AtomicPartnerCall
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (AtomicPartnerCall.run_test())
            System.out.println("\nSUCCESS in AtomicPartnerCall\n");
        else
            System.out.println("\nFAILURE in AtomicPartnerCall\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;
        
        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;
        
        try
        {
            SideA side_a =
                SideA.external_create(new RalphGlobals(params_a));
            SideB side_b =
                SideB.external_create(new RalphGlobals(params_b));
            side_a.set_side_b(side_b);

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