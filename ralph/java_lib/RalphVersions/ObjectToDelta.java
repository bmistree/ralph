package RalphVersions;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import ralph.Util;
import ralph.IReference;

import ralph_protobuffs.DeltaProto.Delta;


public class ObjectToDelta
{
    public final static DoubleToDelta DOUBLE_SERIALIZER =
        new DoubleToDelta();

    public final static StringToDelta STRING_SERIALIZER =
        new StringToDelta();

    public final static BooleanToDelta BOOLEAN_SERIALIZER =
        new BooleanToDelta();

    public final static ReferenceToDelta REFERENCE_SERIALIZER =
        new ReferenceToDelta();

    private static class ReferenceToDelta
        implements IDeltaSerializer<IReference>
    {
        @Override
        public Delta serialize(IReference to_serialize)
        {
            String uuid = to_serialize.uuid();
            Delta.ReferenceType.Builder reference =
                Delta.ReferenceType.newBuilder();
            if (uuid != null)
                reference.setReference(uuid);

            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setReference(reference);
            return proto_buff.build();
        }
    }
        
    private static class DoubleToDelta
        implements IDeltaSerializer<Double>
    {
        @Override
        public Delta serialize(Double to_serialize)
        {
            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setValue(
                ObjectToValueType.DOUBLE_SERIALIZER.serialize_value_type(
                    to_serialize));
            return proto_buff.build();
        }
    }
                                                   
    private static class StringToDelta
        implements IDeltaSerializer<String>
    {
        @Override
        public Delta serialize(String to_serialize)
        {
            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setValue(
                ObjectToValueType.STRING_SERIALIZER.serialize_value_type(
                    to_serialize));
            return proto_buff.build();
        }
    }

    private static class BooleanToDelta
        implements IDeltaSerializer<Boolean>
    {
        @Override
        public Delta serialize(Boolean to_serialize)
        {
            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setValue(
                ObjectToValueType.BOOLEAN_SERIALIZER.serialize_value_type(
                    to_serialize));
            return proto_buff.build();
        }
    }
}