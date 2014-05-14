package ralph;

import java.util.concurrent.atomic.AtomicInteger;

public class UUIDGenerators
{
    public final static RealUUIDGenerator REAL_UUID_GENERATOR =
        new RealUUIDGenerator();
    public final static AtomIntUUIDGenerator ATOM_INT_UUID_GENERATOR =
        new AtomIntUUIDGenerator();
    
    private static class RealUUIDGenerator implements IUUIDGenerator
    {
        @Override
        public String generate_uuid()
        {
            return java.util.UUID.randomUUID().toString();
        }
    }

    private static class AtomIntUUIDGenerator implements IUUIDGenerator
    {
        private final static AtomicInteger atom_int = new AtomicInteger(0);
        @Override
        public String generate_uuid()
        {
            return String.format("%08d",atom_int.incrementAndGet());
        }
    }
}