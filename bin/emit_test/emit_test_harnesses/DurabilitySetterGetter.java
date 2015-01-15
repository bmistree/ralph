package emit_test_harnesses;

import ralph_emitted.AtomicSetterGetterJava.AtomicSetterGetter;
import ralph_emitted.IFaceBasicRalphJava.ISetterGetter;
import RalphConnObj.SingleSideConnection;

import ralph.RalphGlobals;
import ralph.VersioningInfo;
import ralph.RalphObject;
import ralph.Endpoint;



public class DurabilitySetterGetter
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DurabilitySetterGetter\n");
        else
            System.out.println("\nFAILURE in DurabilitySetterGetter\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals.Parameters parameters = new RalphGlobals.Parameters();
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            AtomicSetterGetter endpt = new AtomicSetterGetter(
                ralph_globals,new SingleSideConnection());

            return run_test_from_endpt(endpt,ralph_globals,endpt._uuid);
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
     }

    public static boolean run_test_from_endpt(
        ISetterGetter endpt,RalphGlobals ralph_globals,String endpt_uuid)
        throws Exception
    {
        // testing numbers
        double original_internal_number = endpt.get_number().doubleValue();
        double new_number = original_internal_number + ((double)1);
        for (int i = 0; i < 20; ++i)
        {
            endpt.set_number(new_number);
            double gotten_number = endpt.get_number().doubleValue();
            if (gotten_number != new_number)
                return false;

            new_number += 1.0;
        }

        // testing texts
        String original_internal_text = endpt.get_text();
        String new_text = original_internal_text + "hello";
        for (int i = 0; i < 20; ++i)
        {
            endpt.set_text(new_text);
            String gotten_text = endpt.get_text();
            if (! new_text.equals(gotten_text))
                return false;
            new_text += "hello";
        }

        // testing tfs
        boolean original_internal_bool = endpt.get_tf().booleanValue();
        boolean new_boolean = original_internal_bool;
        for (int i = 0; i < 20; ++i)
        {
            new_boolean = ! new_boolean;
            endpt.set_tf(new_boolean);
            boolean gotten_boolean = endpt.get_tf().booleanValue();
            if (gotten_boolean != new_boolean)
                return false;
        }
        return true;
    }
}