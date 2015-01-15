package emit_test_harnesses;

import ralph.RalphGlobals;
import ralph_emitted.VersionedIFaceJava.VersionedInterface;

public class VersionedIFace
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedIFace\n");
        else
            System.out.println("\nFAILURE in VersionedIFace\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals.Parameters parameters = new RalphGlobals.Parameters();
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            VersionedInterface endpt =
                VersionedInterface.create_single_sided(ralph_globals);
            endpt.initialize();

            return VersionedSetterGetter.run_test_from_endpt(
                endpt,ralph_globals,endpt._uuid);
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}
