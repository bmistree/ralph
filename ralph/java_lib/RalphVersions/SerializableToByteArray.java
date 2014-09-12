package RalphVersions;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import ralph.Util;
import ralph.IReference;



public class SerializableToByteArray
{
    public final static SingletonSerializer SERIALIZER =
        new SingletonSerializer();

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
            return STRING_SERIALIZER.serialize(uuid);
        }
    }
        
    private static class SingletonDoubleSerializer
        implements ILocalDeltaSerializer<Double>
    {
        @Override
        public byte[] serialize(Double to_serialize)
        {
            return SERIALIZER.serialize(to_serialize);
        }
    }
                                                   
    private static class SingletonStringSerializer
        implements ILocalDeltaSerializer<String>
    {
        @Override
        public byte[] serialize(String to_serialize)
        {
            return SERIALIZER.serialize(to_serialize);
        }
    }

    private static class SingletonBooleanSerializer
        implements ILocalDeltaSerializer<Boolean>
    {
        @Override
        public byte[] serialize(Boolean to_serialize)
        {
            return SERIALIZER.serialize(to_serialize);
        }
    }
    
    private static class SingletonSerializer
        implements ILocalDeltaSerializer<Serializable>
    {
        @Override
        public byte[] serialize(Serializable to_serialize)
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = null;
            try
            {
                out = new ObjectOutputStream(bos);   
                out.writeObject(to_serialize);
                return bos.toByteArray();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                Util.logger_assert("Exception while serializing object");
            }
            return null;
        }
    }
}