package RalphVersions;

import RalphDataWrappers.ContainerOpTuple;

import ralph.Util;
import ralph.VersionMapDeltas;
import ralph_local_version_protobuffs.DeltaProto.Delta;


public class MapDeltasToDelta <KeyType>
    implements ILocalDeltaSerializer<
        VersionMapDeltas<KeyType,Object,Object>>
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
        VersionMapDeltas<KeyType,Object,Object> to_serialize)
    {
        Delta.Builder to_return = Delta.newBuilder();
        for (ContainerOpTuple<KeyType,Object,Object> op_tuple :
                 to_serialize.deltas)
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
                key_type_serializer.serialize_value_type(op_tuple.key));
            to_return.addContainerDelta(container_delta);
        }
        return to_return.build();
    }
}