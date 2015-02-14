package emit_test_harnesses;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import ralph_emitted.DurabilityReplayRPCJava.NumHolder;

import RalphDurability.DurabilityReplayer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;
import ralph.DurabilityInfo;


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
            RalphGlobals ralph_globals_a = new RalphGlobals(params_a);
            
            RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
            params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;
            RalphGlobals ralph_globals_b = new RalphGlobals(params_b);
            
            SameHostConnection conn_obj = new SameHostConnection();
            NumHolder original_calling_on =
                NumHolder.external_create(ralph_globals_a,conn_obj);
            NumHolder original_called_on =
                NumHolder.external_create(ralph_globals_b,conn_obj);

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

            while(replayer.step(ralph_globals_a)){}

            // check that final internal values of both endpoints are
            // correct.

            // first, checking rpc caller
            {
                NumHolder replayed_calling_on =
                    (NumHolder)replayer.get_endpoint_if_exists(
                        original_calling_on.uuid());
                
                Double replayed_internal =
                    replayed_calling_on.get_internal_num();
                Double original_internal =
                    original_calling_on.get_internal_num();
            
                if (! replayed_internal.equals(original_internal))
                    return false;
            }

            // second, checking rpc recipient.
            {
                NumHolder replayed_called_on =
                    (NumHolder)replayer.get_endpoint_if_exists(
                        original_called_on.uuid());
                
                Double replayed_internal =
                    replayed_called_on.get_internal_num();
                Double original_internal =
                    original_called_on.get_internal_num();
                
                if (! replayed_internal.equals(original_internal))
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