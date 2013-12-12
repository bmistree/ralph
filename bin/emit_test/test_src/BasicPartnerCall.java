package emit_test_harnesses;

import emit_test_package.BasicPartner.SideA;
import emit_test_package.BasicPartner.SideB;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class BasicPartnerCall
{
    public static void main(String[] args)
    {
        if (BasicPartnerCall.run_test())
            System.out.println("\nSUCCESS in BasicPartnerCall\n");
        else
            System.out.println("\nFAILURE in BasicPartnerCall\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            SideA side_a = new SideA(
                new RalphGlobals(),"a_host_uuid",conn_obj);
            SideB side_b = new SideB(
                new RalphGlobals(),"b_host_uuid",conn_obj);

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