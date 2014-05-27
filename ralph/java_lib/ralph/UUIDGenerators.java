package ralph;

import java.util.concurrent.atomic.AtomicInteger;

public class UUIDGenerators
{
    public final static RealUUIDGenerator REAL_UUID_GENERATOR =
        new RealUUIDGenerator();
    public final static AtomIntUUIDGenerator ATOM_INT_UUID_GENERATOR =
        new AtomIntUUIDGenerator();

    /**
       Regardless of what uuid generator rest of system uses, for
       uuids that need only to be locally unique (eg., for objects),
       use this uuid generator.  If there is still contention, can
       generate uuids with different increments.
     */
    public final static AtomIntUUIDGenerator LOCAL_ATOM_INT_UUID_GENERATOR =
        new AtomIntUUIDGenerator();
    
    public static class RealUUIDGenerator implements IUUIDGenerator
    {
        @Override
        public String generate_uuid()
        {
            return java.util.UUID.randomUUID().toString();
        }
    }

    public static class AtomIntUUIDGenerator implements IUUIDGenerator
    {
        private final AtomicInteger atom_int = new AtomicInteger(0);
        @Override
        public String generate_uuid()
        {
            return String.format("%08d",atom_int.incrementAndGet());
        }
    }
}