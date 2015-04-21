package emit_test_harnesses;

import ralph_emitted.BasicSameHostPartnerJava.RemoteAccessor;
import ralph_emitted.BasicPartnerJava.SideB;

import ralph.RalphGlobals;
import ralph.InternalServiceFactory;

public class BasicSameHostPartnerTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in BasicSameHostPartnerTest\n");
        else
            System.out.println("\nFAILURE in BasicSameHostPartnerTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();

            InternalServiceFactory side_b_service_factory =
                new InternalServiceFactory(SideB.factory, ralph_globals);
            
            RemoteAccessor remote_accessor =
                RemoteAccessor.create_single_sided(ralph_globals);

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