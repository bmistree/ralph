package emit_test_harnesses;

import ralph_emitted.AtomicListJava.TVarListEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;


public class VersionedList
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedList\n");
        else
            System.out.println("\nFAILURE in VersionedList\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            TVarListEndpoint endpt = new TVarListEndpoint(
                ralph_globals, new SingleSideConnection());

            double list_size = endpt.get_size().doubleValue();
            if (list_size != 0)
                return false;

            int num_to_initially_add = 20;
            
            // append to end of list
            for (int i = 0; i < num_to_initially_add; ++i)
            {
                double what_to_insert = i;
                double insertion_index = 0;
                if ((i % 2) == 0)
                {
                    // uses -1 in order to just append to end of list
                    insertion_index = -1;
                }
                endpt.put_number(insertion_index,what_to_insert);
            }

            // remove every other (see += 2 at end)
            for (int i = 0; i < num_to_initially_add; i += 2)
                endpt.remove(0.);

            // Now replay
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.local_version_manager,
                    ralph_globals);

            // now, tries to replay changes to endpoint.  
            TVarListEndpoint replayed_endpt =
                (TVarListEndpoint) VersionUtil.rebuild_endpoint(
                    VersioningInfo.instance.local_version_manager,
                    endpt._uuid,ralph_globals,reconstruction_context);

            if (! replayed_endpt.get_size().equals(endpt.get_size()))
                return false;

            int remaining_size = replayed_endpt.get_size().intValue();
            for (int i = 0; i < remaining_size; ++i)
            {
                double d_i = (double) i;
                double replayed_value =
                    replayed_endpt.get_number(d_i).doubleValue();
                double real_value = endpt.get_number(d_i).doubleValue();
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