package ralph;

import java.util.ArrayList;
import java.util.List;

import RalphVersions.RPCVersionReplayer;
import RalphVersions.ObjectContentsDeserializers;
import RalphVersions.IReconstructionContext;
import RalphVersions.RPCDeserializationReconstructionContext;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.UtilProto.UUID;


public class RPCDeserializationHelper
{
    public static List<RalphObject> deserialize_arguments_list(
        RalphGlobals ralph_globals,Arguments arguments, ActiveEvent act_event)
    {
        List to_return = new ArrayList<RalphObject>();
        RPCVersionReplayer version_replayer =
            new RPCVersionReplayer(arguments);
        IReconstructionContext reconstruction_context =
            new RPCDeserializationReconstructionContext(
                version_replayer,ralph_globals,act_event);

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
}