package emit_test_harnesses;

import ralph_emitted.LocUuidJava.LocUuid;
import ralph.RalphGlobals;

public class LocUuidTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in LocUuidTest\n");
        else
            System.out.println("\nFAILURE in LocUuidTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            LocUuid endpt =
                LocUuid.create_single_sided(ralph_globals);
            String returned = endpt.test();

            if (! returned.equals(ralph_globals.host_uuid))
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}