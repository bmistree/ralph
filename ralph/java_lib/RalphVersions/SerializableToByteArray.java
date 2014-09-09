package RalphVersions;

import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import ralph.Util;

public class SerializableToByteArray
{
    public final static SingletonSerializer SERIALIZER =
        new SingletonSerializer();
    
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