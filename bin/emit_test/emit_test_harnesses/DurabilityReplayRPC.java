package emit_test_harnesses;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import ralph_emitted.DurabilityReplayRPCJava.NumHolder;

import RalphDurability.DurabilityReplayer;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import ralph.DurabilityInfo;
import ralph.InternalServiceFactory;
import ralph.Ralph;

import RalphVersions.VersionUtil;
import RalphVersions.IReconstructionContext;
import RalphVersions.ReconstructionContext;

public class DurabilityReplayRPC
{
    public final static int MAX_NUM_RPCS_PER_TRANSACTION = 20;
    public final static int TCP_CONNECTION_PORT_A = 35855;
    public final static int TCP_CONNECTION_PORT_B = 35856;
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DurabilityReplayRPC\n");
        else
            System.out.println("\nFAILURE in DurabilityReplayRPC\n");
    }

    public static boolean run_test()
    {
        Random rand = new Random();
        try
        {
            RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
            params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;
            RalphGlobals globals_a = new RalphGlobals(params_a);

            RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
            params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;
            RalphGlobals globals_b = new RalphGlobals(params_b);

            // connect hosts a and b, via a tcp connection
            Thread.sleep(500);
            Ralph.tcp_connect("127.0.0.1", TCP_CONNECTION_PORT_B, globals_a);
            Thread.sleep(500);

            // Instantiate NumHolder and have it build a remote copy
            NumHolder original_calling_on = NumHolder.external_create(globals_a);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(NumHolder.factory, globals_a);
            original_calling_on.install_partner(service_receiver_factory_to_send);

            for (int i = 0; i < 20; ++i)
            {
                int rpcs_per_transaction =
                    rand.nextInt(MAX_NUM_RPCS_PER_TRANSACTION);
                original_calling_on.make_request(
                    new Double(rpcs_per_transaction));
            }

            // Now, try to replay from durability file
            DurabilityReplayer replayer =
                (DurabilityReplayer)DurabilityInfo.instance.durability_replayer;
            
            while(replayer.step(globals_a)){}

            NumHolder replayed_calling_on =
                (NumHolder) replayer.get_endpoint_if_exists(original_calling_on.uuid());

            Double replayed_internal =
                replayed_calling_on.get_internal_num();
            Double original_internal =
                original_calling_on.get_internal_num();

            if (! replayed_internal.equals(original_internal))
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