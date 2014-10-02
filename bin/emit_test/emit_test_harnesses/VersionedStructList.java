package emit_test_harnesses;

import ralph_emitted.ReplayStructListJava.ReplayStructList;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;


public class VersionedStructList
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedStructList\n");
        else
            System.out.println("\nFAILURE in VersionedStructList\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            ReplayStructList endpt = new ReplayStructList(
                ralph_globals, new SingleSideConnection());
            
            int num_to_initially_add = 20;
            
            // append to end of list
            for (int i = 0; i < num_to_initially_add; ++i)
                endpt.add_internal((double)i);

            VersioningInfo.instance.version_saver.flush();
            
            // Now replay
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.version_replayer,
                    ralph_globals);

            // now, tries to replay changes to endpoint.  
            ReplayStructList replayed_endpt =
                (ReplayStructList) VersionUtil.rebuild_endpoint(
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