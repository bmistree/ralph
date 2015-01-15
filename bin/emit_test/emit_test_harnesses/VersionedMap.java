package emit_test_harnesses;

import ralph_emitted.AtomicMapJava.TVarMapEndpoint;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;

public class VersionedMap
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedMap\n");
        else
            System.out.println("\nFAILURE in VersionedMap\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            TVarMapEndpoint endpt =
                TVarMapEndpoint.create_single_sided(ralph_globals);

            int highest_index_to_add = 20;
            
            for (int i = 0; i <= highest_index_to_add; ++i)
                endpt.put_number((double)i, (double)i);

            for (int i = 0; i < highest_index_to_add/2; ++i)
                endpt.remove((double)i);
            
            // map should ultimately contain indices 11-19, with
            // values equal to keys.

            VersioningInfo.instance.version_saver.flush();
            
            IReconstructionContext reconstruction_context =
                new ReconstructionContext(
                    VersioningInfo.instance.version_replayer,
                    ralph_globals);

            // now, tries to replay changes to endpoint.  
            TVarMapEndpoint replayed_endpt =
                (TVarMapEndpoint) VersionUtil.rebuild_endpoint(
                    endpt._uuid,ralph_globals,reconstruction_context);

            if (! replayed_endpt.get_size().equals(endpt.get_size()))
                return false;

            for (int i = 0; i <= highest_index_to_add; ++i)
            {
                double d_i = (double)i;
                boolean replayed_contains =
                    replayed_endpt.contains_index(d_i).booleanValue();
                boolean real_contains =
                    endpt.contains_index(d_i).booleanValue();

                if (real_contains != replayed_contains)
                    return false;

                if (real_contains)
                {
                    double replayed_num =
                        replayed_endpt.get_number(d_i).doubleValue();
                    double real_num =
                        endpt.get_number(d_i).doubleValue();
                    if (replayed_num != real_num)
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