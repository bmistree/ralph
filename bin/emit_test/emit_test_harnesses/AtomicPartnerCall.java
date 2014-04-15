package emit_test_harnesses;

import ralph_emitted.AtomicPartner.SideA;
import ralph_emitted.AtomicPartner.SideB;
import RalphConnObj.SameHostConnection;
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
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            SideA side_a = new SideA(
                new RalphGlobals(TCP_CONNECTION_PORT_A),conn_obj);
            SideB side_b = new SideB(
                new RalphGlobals(TCP_CONNECTION_PORT_B),conn_obj);

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