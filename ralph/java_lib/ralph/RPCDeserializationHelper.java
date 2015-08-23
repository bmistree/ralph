package ralph;

import java.util.ArrayList;
import java.util.List;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.UtilProto.UUID;

import RalphVersions.RPCVersionReplayer;
import RalphVersions.ObjectContentsDeserializers;
import RalphVersions.IReconstructionContext;
import RalphVersions.RPCDeserializationReconstructionContext;

import ralph.ExecutionContext.ExecutionContext;


public class RPCDeserializationHelper
{
    public static List<RalphObject> deserialize_arguments_list(
        RalphGlobals ralph_globals,Arguments arguments,
        ExecutionContext exec_ctx)
    {
        List to_return = new ArrayList<RalphObject>();
        RPCVersionReplayer version_replayer =
            new RPCVersionReplayer(arguments);
        IReconstructionContext reconstruction_context =
            new RPCDeserializationReconstructionContext(
                version_replayer,ralph_globals,exec_ctx);

        for (UUID arg_uuid : arguments.getArgumentUuidsList())
        {
            String obj_uuid = arg_uuid.getData();
            ObjectContents obj_contents =
                version_replayer.get_object_contents(obj_uuid);
            //// DEBUG
            if (obj_contents == null)
            {
                Util.logger_assert(
                    "Require obj_contents when deserializng rpc arg");
            }
            //// END DEBUG

            RalphObject ro = reconstruction_context.get_constructed_object(
                obj_uuid, null);

            //// DEBUG
            if (ro == null)
            {
                Util.logger_assert(
                    "Received a ralphobject that we could not deserialize " +
                    "for rpc.");
            }
            //// END DEBUG

            to_return.add(ro);
        }

        return to_return;
    }

    public static RalphObject return_args_to_ralph_object (
        Arguments returned_objs_proto, RalphGlobals ralph_globals,
        ExecutionContext exec_ctx)
    {
        if (returned_objs_proto == null)
            return null;

        // FIXME: only need a single element, not an entire list
        // of returned objects.  Using this call instead so that
        // can repurpose deserialization code
        List<RalphObject> to_return_list =
            RPCDeserializationHelper.deserialize_arguments_list(
                ralph_globals,returned_objs_proto,exec_ctx);
        //// DEBUG
        if (to_return_list.size() != 1)
        {
            Util.logger_assert(
                "Should only be able to return single object from rpc.");
        }
        //// END DEBUG

        return to_return_list.get(0);
    }
}