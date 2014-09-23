package emit_test_harnesses;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;

import ralph_emitted.BasicRalphJava.SetterGetter;
import ralph_emitted.IFaceBasicRalphJava.ISetterGetter;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.VersioningInfo;
import ralph.RalphObject;
import ralph.Endpoint;
import ralph.EndpointConstructorObj;
import RalphVersions.EndpointInitializationHistory;
import RalphVersions.ObjectHistory;
import RalphVersions.ObjectContentsDeserializers;
import RalphVersions.VersionUtil;
import RalphVersions.ReconstructionContext;
import RalphVersions.IReconstructionContext;

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
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            SetterGetter endpt = new SetterGetter(
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

        IReconstructionContext reconstruction_context =
            new ReconstructionContext(
                VersioningInfo.instance.local_version_replayer,
                ralph_globals);

        // now, tries to replay changes to endpoint.  
        ISetterGetter replayed_endpt =
            (ISetterGetter) VersionUtil.rebuild_endpoint(
                VersioningInfo.instance.local_version_replayer,
                endpt_uuid,ralph_globals,reconstruction_context);

        // NOTE: non-atomics are not under version control, and
        // operations to them are therefore not logged.
        // if (!endpt.get_text().equals(replayed_endpt.get_text()))
        //     return false;

        if (!endpt.get_number().equals(replayed_endpt.get_number()))
            return false;

        if (!endpt.get_tf().equals(replayed_endpt.get_tf()))
            return false;

        return true;
    }
    
}