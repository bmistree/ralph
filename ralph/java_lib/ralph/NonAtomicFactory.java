package ralph;

public interface NonAtomicFactory<T> {
    public NonAtomicObject<T> construct(
        String host_uuid, boolean peered, Object init_val);
}
