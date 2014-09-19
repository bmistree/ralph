package emit_test_harnesses;

import ralph_emitted.ReplayStructMapJava.ReplayStructMap;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;


public class VersionedStructMap
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedStructMap\n");
        else
            System.out.println("\nFAILURE in VersionedStructMap\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            ReplayStructMap endpt = new ReplayStructMap(
                ralph_globals, new SingleSideConnection());
            
            int num_to_initially_add = 20;
            
            // append to end of list
            for (int i = 0; i < num_to_initially_add; ++i)
                endpt.add_internal((double)i);

            
            // Now replay
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.local_version_replayer,
                    ralph_globals);

            // now, tries to replay changes to endpoint.  
            ReplayStructMap replayed_endpt =
                (ReplayStructMap) VersionUtil.rebuild_endpoint(
                    VersioningInfo.instance.local_version_replayer,
                    endpt._uuid,ralph_globals,reconstruction_context);

            if (! replayed_endpt.get_size().equals(endpt.get_size()))
                return false;

            int remaining_size = replayed_endpt.get_size().intValue();
            for (int i = 0; i < remaining_size; ++i)
            {
                double d_i = (double) i;
                Double replayed_value =
                    replayed_endpt.get_internal_value(d_i);
                Double real_value =
                    endpt.get_internal_value(d_i);

                // Having this condition followed by the next means
                // that we do not really have to test for null to
                // ensure correctness.
                if (replayed_value == real_value)
                    continue;
                
                if (! real_value.equals(replayed_value))
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