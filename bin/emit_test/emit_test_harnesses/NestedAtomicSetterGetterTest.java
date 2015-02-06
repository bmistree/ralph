package emit_test_harnesses;

import ralph_emitted.NestedAtomicSetterGetterJava.NestedAtomicSetterGetter;
import ralph_emitted.IFaceBasicRalphJava.ISetterGetter;
import ralph.RalphGlobals;

public class NestedAtomicSetterGetterTest
{
    public static void main(String[] args)
    {
        if (run_test())
        {
            System.out.println(
                "\nSUCCESS in NestedAtomicSetterGetterTest\n");
        }
        else
        {
            System.out.println(
                "\nFAILURE in NestedAtomicSetterGetterTest\n");
        }
    }
    
    public static boolean run_test()
    {
        try
        {
            ISetterGetter endpt =
                NestedAtomicSetterGetter.create_single_sided(
                    new RalphGlobals());
            BasicSetterGetter.isetter_getter_runs(endpt);
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}