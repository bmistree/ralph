package RalphVersions;

import java.util.List;

import RalphDataWrappers.ContainerOpTuple;

import ralph.Util;
import ralph.VersionMapDeltas;
import ralph_local_version_protobuffs.DeltaProto.Delta;


public class MapDeltasToDelta <KeyType>
    implements ILocalDeltaSerializer<VersionMapDeltas>
{
    public static final MapDeltasToDelta<Double> DOUBLE_KEYED_MAP_DELTA_SERIALIZER =
        new MapDeltasToDelta(ObjectToValueType.DOUBLE_SERIALIZER);
    public static final MapDeltasToDelta<String> STRING_KEYED_MAP_DELTA_SERIALIZER =
        new MapDeltasToDelta(ObjectToValueType.STRING_SERIALIZER);
    public static final MapDeltasToDelta<Boolean> BOOLEAN_KEYED_MAP_DELTA_SERIALIZER =
        new MapDeltasToDelta(ObjectToValueType.BOOLEAN_SERIALIZER);
    
    private final ILocalValueTypeSerializer<KeyType> key_type_serializer;

    public MapDeltasToDelta(
        ILocalValueTypeSerializer<KeyType> key_type_serializer)
    {
        this.key_type_serializer = key_type_serializer;
    }

    @Override
    public Delta serialize(
        VersionMapDeltas to_serialize)
    {
        Delta.Builder to_return = Delta.newBuilder();
        List<ContainerOpTuple> to_iter_over = to_serialize.deltas;
        for (ContainerOpTuple op_tuple : to_iter_over)
        {
            Delta.ContainerDelta.Builder container_delta =
                Delta.ContainerDelta.newBuilder();

            if (op_tuple.type == ContainerOpTuple.OpType.DELETE)
            {
                container_delta.setOpType(Delta.ContainerOpType.DELETE);
            }
            else if (op_tuple.type == ContainerOpTuple.OpType.ADD)
            {
                container_delta.setOpType(Delta.ContainerOpType.ADD);
                Delta.ReferenceType.Builder reference =
                    Delta.ReferenceType.newBuilder();
                reference.setReference(
                    op_tuple.what_added_or_removed.uuid());
                
                container_delta.setWhatAddedOrWritten(reference);
            }
            else if (op_tuple.type == ContainerOpTuple.OpType.WRITE)
            {
                container_delta.setOpType(Delta.ContainerOpType.WRITE);
                Delta.ReferenceType.Builder reference =
                    Delta.ReferenceType.newBuilder();
                reference.setReference(
                    op_tuple.what_added_or_removed.uuid());
                container_delta.setWhatAddedOrWritten(reference);
            }
            else if (op_tuple.type == ContainerOpTuple.OpType.CLEAR)
            {
                container_delta.setOpType(Delta.ContainerOpType.CLEAR);
            }
            //// DEBUG
            else
            {
                Util.logger_assert("Unknown container op type");
            }
            //// END DEBUG

            
            container_delta.setKey(
                key_type_serializer.serialize_value_type(
                    (KeyType)op_tuple.key));
            to_return.addContainerDelta(container_delta);
        }
        return to_return.build();
    }
}