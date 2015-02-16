package emit_test_harnesses;

import ralph_emitted.MixedAccessesJava.MixedAccesses;
import ralph.RalphGlobals;

public class MixedAccessesTester
{
    public static void main(String[] args)
    {
        if (MixedAccessesTester.run_test())
            System.out.println("\nSUCCESS in MixedAccessesTester\n");
        else
            System.out.println("\nFAILURE in MixedAccessesTester\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            MixedAccesses endpt =
                MixedAccesses.create_single_sided(new RalphGlobals());

            for (int i = 0; i < 10; ++i)
                endpt.read_size();

            for (int i = 0; i < 10; ++i)
                endpt.append_element(new Double(i));
            
            for (int i = 0; i < 10; ++i)
                endpt.read_size();
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}