package emit_test_harnesses;

import java.io.IOException;
import java.util.List;

import ralph_emitted.BasicPartnerJava.SideB;
import ralph_emitted.WrappedRemoteJava.SingleSidedHolder;
import ralph_emitted.WrappedRemoteJava.InternalHolder;
import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.EndpointConstructorObj;
import ralph.Endpoint;
import ralph.Ralph;
import ralph.InternalServiceFactory;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityReplayContext;

public class WrappedSameHostRemoteCall
{
    private static final String HOST_NAME = "localhost";

    public static void main(String[] args)
    {
        if (WrappedSameHostRemoteCall.run_test())
            System.out.println("\nSUCCESS in WrappedSameHostRemoteCall\n");
        else
            System.out.println("\nFAILURE in WrappedSameHostRemoteCall\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            SingleSidedHolder single_holder =
                SingleSidedHolder.create_single_sided(ralph_globals);

            InternalServiceFactory side_b_service_factory =
                new InternalServiceFactory(SideB.factory, ralph_globals);
            single_holder.install_remote(side_b_service_factory);

            for (int i = 0; i < 20; ++i)
                single_holder.issue_call();

            int internal_number = single_holder.get_number().intValue();
            if (internal_number != 20)
                return false;
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}