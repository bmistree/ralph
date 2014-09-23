package emit_test_harnesses;

import ralph_emitted.ReplayEnumJava.EnumHolder;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;

public class VersionedEnum
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedEnum\n");
        else
            System.out.println("\nFAILURE in VersionedEnum\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            EnumHolder endpt = new EnumHolder(
                ralph_globals, new SingleSideConnection());

            for (int i = 0; i < 20; ++i)
                endpt.update();

            // Now replay
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.local_version_replayer,
                    ralph_globals);
            
            VersioningInfo.instance.local_version_saver.flush();
            
            // now, tries to replay changes to endpoint.  
            EnumHolder replayed_endpt =
                (EnumHolder) VersionUtil.rebuild_endpoint(
                    VersioningInfo.instance.local_version_replayer,
                    endpt._uuid,ralph_globals,reconstruction_context);

            if (! replayed_endpt.get_day().equals(endpt.get_day()))
                return false;
            
            if (! replayed_endpt.get_history_size().equals(endpt.get_history_size()))
                return false;
            
            int history_size = replayed_endpt.get_history_size().intValue();
            for (int i = 0; i < history_size; ++i)
            {
                double d_i = (double) i;

                if (! replayed_endpt.get_day_on_history_index(d_i).equals(
                        endpt.get_day_on_history_index(d_i)))
                {
                    return false;
                }
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