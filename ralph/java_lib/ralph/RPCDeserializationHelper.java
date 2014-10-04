package ralph;

import java.util.ArrayList;
import java.util.List;

import RalphVersions.RPCVersionReplayer;
import RalphVersions.ObjectContentsDeserializers;
import RalphVersions.IReconstructionContext;
import RalphVersions.ReconstructionContext;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.UtilProto.UUID;


public class RPCDeserializationHelper
{
    public static List<RalphObject> deserialize_arguments_list(
        RalphGlobals ralph_globals,Arguments arguments)
    {
        List to_return = new ArrayList<RalphObject>();
        RPCVersionReplayer version_replayer =
            new RPCVersionReplayer(arguments);
        IReconstructionContext reconstruction_context =
            new ReconstructionContext(version_replayer,ralph_globals);

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
            
            RalphObject ro = ObjectContentsDeserializers.deserialize(
                obj_contents,ralph_globals,reconstruction_context);
            to_return.add(ro);
        }
        
        return to_return;
    }
}