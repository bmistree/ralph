package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.EndpointConstructorObj;
import ralph.Endpoint;
import ralph.RalphObject;
import RalphVersions.EndpointInitializationHistory.NameUUIDTuple;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

public class VersionUtil
{
    /**
       @param endpt_constructor_class_name_to_obj --- Keys are
       endpoint constructor object class names, values are
       EndpointConstructorObjs.
     */
    public static Endpoint rebuild_endpoint(
        ILocalVersionManager local_version_manager,
        String endpoint_uuid,
        RalphGlobals ralph_globals)
    {
        EndpointInitializationHistory endpt_history =
            local_version_manager.get_endpoint_initialization_history(
                endpoint_uuid);
        EndpointConstructorObj endpt_constructor_obj =
            local_version_manager.get_endpoint_constructor_obj(
                endpt_history.endpoint_constructor_class_name);

        // repopulate all initial ralph objects that get placed in
        // endpoint.
        List<RalphObject> endpt_initialization_vars =
            new ArrayList<RalphObject>();
        
        for (NameUUIDTuple name_uuid_tuple : endpt_history.variable_list)
        {
            String obj_uuid = name_uuid_tuple.uuid;
            ObjectHistory obj_history =
                local_version_manager.get_full_object_history(obj_uuid);

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