package emit_test_harnesses;

import ralph_emitted.ReplayNonAtomicStructMapJava.ReplayNonAtomicStructMap;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;


public class VersionedNonAtomicStructMap
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedNonAtomicStructMap\n");
        else
            System.out.println("\nFAILURE in VersionedNonAtomicStructMap\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            ReplayNonAtomicStructMap endpt = new ReplayNonAtomicStructMap(
                ralph_globals, new SingleSideConnection());
            
            int num_to_initially_add = 20;
            
            // append to end of list
            for (int i = 0; i < num_to_initially_add; ++i)
                endpt.add_internal((double)i);

            
            // Now replay
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.local_version_manager,
                    ralph_globals);

            // now, tries to replay changes to endpoint.  
            ReplayNonAtomicStructMap replayed_endpt =
                (ReplayNonAtomicStructMap) VersionUtil.rebuild_endpoint(
                    VersioningInfo.instance.local_version_manager,
                    endpt._uuid,ralph_globals,reconstruction_context);

            if (! replayed_endpt.get_size().equals(endpt.get_size()))
                return false;

            int remaining_size = replayed_endpt.get_size().intValue();
            for (int i = 0; i < remaining_size; ++i)
            {
                double d_i = (double) i;
                double replayed_value =
                    replayed_endpt.get_internal_value(d_i).doubleValue();
                double real_value =
                    endpt.get_internal_value(d_i).doubleValue();
                if (real_value != replayed_value)
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