package emit_test_harnesses;

import ralph_emitted.AtomicMapJava.TVarMapEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

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
            TVarMapEndpoint endpt = new TVarMapEndpoint(
                new RalphGlobals(), new SingleSideConnection());

            for (int i = 0; i < 20; ++i)
                endpt.put_number((double)i, (double)i);

            for (int i = 0; i < 10; ++i)
                endpt.remove((double)i);
            
            // map should ultimately contain indices 11-19, with
            // values equal to keys.

            // FIXME: must now replay
            
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}