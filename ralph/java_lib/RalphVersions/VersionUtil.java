package RalphVersions;

import java.util.List;
import java.util.ArrayList;

import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.EndpointConstructorObj;
import ralph.Endpoint;
import ralph.RalphObject;
import RalphVersions.EndpointInitializationHistory.NameUUIDTuple;

public class VersionUtil
{
    /**
       @param endpt_constructor_class_name_to_obj --- Keys are
       endpoint constructor object class names, values are
       EndpointConstructorObjs.
     */
    public static Endpoint rebuild_endpoint(
        ILocalVersionReplayer local_version_replayer,
        String endpoint_uuid,
        RalphGlobals ralph_globals,
        IReconstructionContext reconstruction_context)
    {
        EndpointInitializationHistory endpt_history =
            local_version_replayer.get_endpoint_initialization_history(
                endpoint_uuid);
        EndpointConstructorObj endpt_constructor_obj =
            local_version_replayer.get_endpoint_constructor_obj(
                endpt_history.endpoint_constructor_class_name);
        
        // repopulate all initial ralph objects that get placed in
        // endpoint.
        List<RalphObject> endpt_initialization_vars =
            new ArrayList<RalphObject>();
        
        for (NameUUIDTuple name_uuid_tuple : endpt_history.variable_list)
        {
            String obj_uuid = name_uuid_tuple.uuid;
            // putting null in for second parameter in order to play
            // all the way to end.
            RalphObject ralph_object =
                reconstruction_context.get_constructed_object(obj_uuid,null);
            endpt_initialization_vars.add(ralph_object);
        }

        return endpt_constructor_obj.construct(
            ralph_globals,new SingleSideConnection(),
            endpt_initialization_vars);
    }
}