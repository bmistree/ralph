package RalphVersions;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import ralph.Util;
import ralph.IReference;

import ralph_local_version_protobuffs.ObjectProto.Object;


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
            if (uuid == null)
                uuid = "null";

            Object.Builder proto_buff = Object.newBuilder();
            proto_buff.setReference(uuid);
            return proto_buff.build().toByteArray();
        }
    }
        
    private static class SingletonDoubleSerializer
        implements ILocalDeltaSerializer<Double>
    {
        @Override
        public byte[] serialize(Double to_serialize)
        {
            Object.Builder proto_buff = Object.newBuilder();
            proto_buff.setNum(to_serialize.doubleValue());
            return proto_buff.build().toByteArray();
        }
    }
                                                   
    private static class SingletonStringSerializer
        implements ILocalDeltaSerializer<String>
    {
        @Override
        public byte[] serialize(String to_serialize)
        {
            Object.Builder proto_buff = Object.newBuilder();
            proto_buff.setText(to_serialize);
            return proto_buff.build().toByteArray();
        }
    }

    private static class SingletonBooleanSerializer
        implements ILocalDeltaSerializer<Boolean>
    {
        @Override
        public byte[] serialize(Boolean to_serialize)
        {
            Object.Builder proto_buff = Object.newBuilder();
            proto_buff.setTf(to_serialize.booleanValue());
            return proto_buff.build().toByteArray();
        }
    }
}