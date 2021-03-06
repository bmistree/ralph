package RalphVersions;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.DeltaProto.Delta.ReferenceType;

import RalphExceptions.BackoutException;

import ralph.RalphGlobals;
import ralph.RalphObject;
import ralph.Util;
import ralph.ActiveEvent;
import ralph.ExecutionContext.ExecutionContext;
import ralph.Endpoint;
import ralph.Variables;


/**
   Used to deserialize rpc arguments.  Difference between this and
   ReconstructionContext is that this code logs any changes to
   variables from deserialization so that can later replay.
 */

public class RPCDeserializationReconstructionContext
    implements IReconstructionContext
{
    private final IVersionReplayer version_replayer;
    private final RalphGlobals ralph_globals;
    private final ExecutionContext exec_ctx;

    public RPCDeserializationReconstructionContext(
        IVersionReplayer _version_replayer,
        RalphGlobals _ralph_globals,ExecutionContext _exec_ctx)
    {
        version_replayer = _version_replayer;
        ralph_globals = _ralph_globals;
        exec_ctx = _exec_ctx;
    }

    @Override
    public Endpoint get_endpt(String uuid)
    {
        return exec_ctx.get_endpt_if_exists(uuid);
    }

    @Override
    public IVersionReplayer get_version_replayer()
    {
        return version_replayer;
    }

    @Override
    public RalphObject get_constructed_object(
        String obj_uuid, Long lamport_timestamp_before_or_during)
    {
        ObjectHistory obj_history =
            version_replayer.get_full_object_history(obj_uuid);

        //// DEBUG
        if (obj_history == null)
        {
            Util.logger_assert(
                "Could not find object history for target obj_uuid");
        }
        //// END DEBUG

        ObjectContents initial_contents =
            obj_history.initial_construction_contents;

        RalphObject ralph_object = null;

        try
        {
            if (exec_ctx.is_replaying()) {
                if (initial_contents.hasInterface()) {
                    ObjectContents.Interface interface_msg =
                        initial_contents.getInterface();
                    ReferenceType ref_type = interface_msg.getRefType();
                    Endpoint endpt = get_endpt(ref_type.getReference());
                    if (endpt != null) {
                        RalphObject returner =
                            new Variables.NonAtomicInterfaceVariable<Endpoint>(
                                false, endpt, ralph_globals);
                        return returner;
                    }
                }
            }

            ralph_object =
                ObjectContentsDeserializers.deserialize(
                    initial_contents,ralph_globals,this);

            // plays deltas forward when reconstructing object.
            ralph_object.deserialize(
                this,obj_history,lamport_timestamp_before_or_during,
                exec_ctx.curr_act_evt());
        }
        catch (BackoutException ex)
        {
            // FIXME: consider cleaner way of handling backout here.
            ex.printStackTrace();
            Util.logger_assert(
                "Should never receive a backout exception when " +
                "deserializing variables.");
        }
        return ralph_object;
    }
}