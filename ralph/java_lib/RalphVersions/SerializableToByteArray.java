package RalphVersions;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import ralph.Util;
import ralph.IReference;

import ralph_local_version_protobuffs.DeltaProto.Delta;


public class SerializableToByteArray
{
    public final static SingletonDoubleSerializer DOUBLE_SERIALIZER =
        new SingletonDoubleSerializer();

    public final static SingletonStringSerializer STRING_SERIALIZER =
        new SingletonStringSerializer();

    public final static SingletonBooleanSerializer BOOLEAN_SERIALIZER =
        new SingletonBooleanSerializer();

    public final static SingletonReferenceSerializer REFERENCE_SERIALIZER =
        new SingletonReferenceSerializer();


    private static class SingletonReferenceSerializer
        implements ILocalDeltaSerializer<IReference>
    {
        @Override
        public byte[] serialize(IReference to_serialize)
        {
            String uuid = to_serialize.uuid();
            Delta.ReferenceType.Builder reference =
                Delta.ReferenceType.newBuilder();
            if (uuid != null)
                reference.setReference(uuid);

            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setReference(reference);
            return proto_buff.build().toByteArray();
        }
    }
        
    private static class SingletonDoubleSerializer
        implements ILocalDeltaSerializer<Double>
    {
        @Override
        public byte[] serialize(Double to_serialize)
        {
            Delta.ValueType.Builder value =
                Delta.ValueType.newBuilder();
            value.setNum(to_serialize.doubleValue());
            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setValue(value);
            return proto_buff.build().toByteArray();
        }
    }
                                                   
    private static class SingletonStringSerializer
        implements ILocalDeltaSerializer<String>
    {
        @Override
        public byte[] serialize(String to_serialize)
        {
            Delta.ValueType.Builder value =
                Delta.ValueType.newBuilder();
            value.setText(to_serialize);
            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setValue(value);
            return proto_buff.build().toByteArray();
        }
    }

    private static class SingletonBooleanSerializer
        implements ILocalDeltaSerializer<Boolean>
    {
        @Override
        public byte[] serialize(Boolean to_serialize)
        {
            Delta.ValueType.Builder value =
                Delta.ValueType.newBuilder();
            value.setTf(to_serialize.booleanValue());
            Delta.Builder proto_buff = Delta.newBuilder();
            proto_buff.setValue(value);
            return proto_buff.build().toByteArray();
        }
    }
}