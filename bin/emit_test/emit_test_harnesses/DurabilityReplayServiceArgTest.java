package emit_test_harnesses;

import ralph_emitted.DurabilityReplayServiceArgJava.SomeService;

import RalphDurability.DurabilityReplayer;
import ralph.DurabilityInfo;
import ralph.RalphGlobals;


public class DurabilityReplayServiceArgTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DurabilityReplayServiceArgTest\n");
        else
            System.out.println("\nFAILURE in DurabilityReplayServiceArgTest\n");
    }

    public static boolean run_test()
    {
        try {
            RalphGlobals globals = new RalphGlobals();
            SomeService service = SomeService.create_single_sided(globals);
            service.assign_service(service);
            service.get_remote_num();

            // Now, try to replay from durability log
            DurabilityReplayer replayer =
                (DurabilityReplayer)DurabilityInfo.instance.durability_replayer;

            while(replayer.step(globals)) { }
            SomeService replayed_calling_on =
                (SomeService) replayer.get_endpoint_if_exists(service.uuid());
            return replayed_calling_on.get_remote_num().equals(0.0);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}