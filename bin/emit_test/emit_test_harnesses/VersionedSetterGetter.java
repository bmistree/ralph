package emit_test_harnesses;

import ralph_emitted.BasicRalphJava.SetterGetter;
import ralph_emitted.IFaceBasicRalphJava.ISetterGetter;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import RalphVersions.InMemoryLocalVersionManager;

/**
   Record all changes to endpoint, and then try to replay them.  Note:
   currently, only logging changes.  Have not actually replayed yet.
 */

public class VersionedSetterGetter
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionedSetterGetter\n");
        else
            System.out.println("\nFAILURE in VersionedSetterGetter\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals.Parameters parameters = new RalphGlobals.Parameters();
            VersioningInfo.instance.local_version_manager =
                new InMemoryLocalVersionManager();
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            ISetterGetter endpt = new SetterGetter(
                ralph_globals,new SingleSideConnection());

            
            // testing numbers
            double original_internal_number = endpt.get_number().doubleValue();
            for (int i = 0; i < 20; ++i)
            {
                double new_number = original_internal_number + ((double)1);
                endpt.set_number(new_number);
                double gotten_number = endpt.get_number().doubleValue();
                if (gotten_number != new_number)
                    return false;
            }

            // testing texts
            String original_internal_text = endpt.get_text();
            for (int i = 0; i < 20; ++i)
            {
                String new_text = original_internal_text + "hello";
                endpt.set_text(new_text);
                String gotten_text = endpt.get_text();
                if (! new_text.equals(gotten_text))
                    return false;
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
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}