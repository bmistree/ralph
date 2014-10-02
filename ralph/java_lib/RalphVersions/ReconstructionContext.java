package RalphVersions;

import ralph.RalphGlobals;
import ralph.RalphObject;

import ralph_version_protobuffs.ObjectContentsProto.ObjectContents;

public class ReconstructionContext implements IReconstructionContext
{
    private final ILocalVersionReplayer local_version_replayer;
    private final RalphGlobals ralph_globals;
    
    public ReconstructionContext (
        ILocalVersionReplayer _local_version_replayer,
        RalphGlobals _ralph_globals)
    {
        local_version_replayer = _local_version_replayer;
        ralph_globals = _ralph_globals;
    }

    @Override
    public ILocalVersionReplayer get_local_version_replayer()
    {
        return local_version_replayer;
    }
    
    @Override
    public RalphObject get_constructed_object(
        String obj_uuid, Long lamport_timestamp_before_or_during)
    {
        ObjectHistory obj_history =
            local_version_replayer.get_full_object_history(obj_uuid);

        ObjectContents initial_contents =
            obj_history.initial_construction_contents;
            
        RalphObject ralph_object =
            ObjectContentsDeserializers.deserialize(
                initial_contents,ralph_globals,this);


        // plays deltas forward when reconstructing object.  
        ralph_object.replay(
            this,obj_history,lamport_timestamp_before_or_during);
        return ralph_object;
    }
}