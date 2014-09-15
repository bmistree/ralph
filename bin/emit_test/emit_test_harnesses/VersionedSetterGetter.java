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
import RalphVersions.InMemoryLocalVersionManager;
import RalphVersions.EndpointInitializationHistory;
import RalphVersions.EndpointInitializationHistory.NameUUIDTuple;
import RalphVersions.ObjectHistory;
import RalphVersions.ObjectContentsDeserializers;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

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
            InMemoryLocalVersionManager in_memory_version_manager =
                new InMemoryLocalVersionManager();
            VersioningInfo.instance.local_version_manager =
                in_memory_version_manager;
            RalphGlobals ralph_globals = new RalphGlobals(parameters);

            SetterGetter endpt = new SetterGetter(
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

            // now, tries to replay changes to endpoint.  First,
            // identify the variables that were initialized as part of
            Map<String,EndpointConstructorObj> constructor_map =
                new HashMap<String,EndpointConstructorObj>();
            constructor_map.put(
                SetterGetter.factory.getClass().getName(),
                SetterGetter.factory);
            
            ISetterGetter replayed_endpt = (ISetterGetter) rebuild_endpoint(
                in_memory_version_manager,endpt._uuid,
                constructor_map,ralph_globals);

            if (!endpt.get_text().equals(replayed_endpt.get_text()))
                return false;

            if (!endpt.get_number().equals(replayed_endpt.get_number()))
                return false;
            
            if (!endpt.get_tf().equals(replayed_endpt.get_tf()))
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    /**
       @param endpt_constructor_class_name_to_obj --- Keys are
       endpoint constructor object class names, values are
       EndpointConstructorObjs.
     */
    public static Endpoint rebuild_endpoint(
        InMemoryLocalVersionManager local_version_manager,
        String endpoint_uuid,
        Map<String,EndpointConstructorObj> endpt_constructor_class_name_to_obj,
        RalphGlobals ralph_globals)
    {
        EndpointInitializationHistory endpt_history =
            local_version_manager.get_endpoint_initialization_history(
                endpoint_uuid);
        EndpointConstructorObj endpt_constructor_obj =
            endpt_constructor_class_name_to_obj.get(
                endpt_history.endpoint_constructor_class_name);

        // repopulate all initial ralph objects that get placed in
        // endpoint.
        List<RalphObject> endpt_initialization_vars =
            new ArrayList<RalphObject>();
        
        for (NameUUIDTuple name_uuid_tuple : endpt_history.variable_list)
        {
            String obj_uuid = name_uuid_tuple.uuid;
            ObjectHistory obj_history =
                local_version_manager.get_object_history(obj_uuid);

            ObjectContents initial_contents =
                obj_history.initial_construction_contents;
            
            RalphObject ralph_object =
                ObjectContentsDeserializers.deserialize(
                    initial_contents,ralph_globals);
            // plays deltas forward when reconstructing object.  note
            // that using null for reconstruction context, because
            // should be able to reconstruct only from history.  Also
            // note that using null as third argument, indicating that
            // we should replay all changes on top of object.
            ralph_object.replay(null,obj_history,null);
            
            endpt_initialization_vars.add(ralph_object);
        }

        return endpt_constructor_obj.construct(
            ralph_globals,new SingleSideConnection(),
            endpt_initialization_vars);
    }
}