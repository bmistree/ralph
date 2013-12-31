package ralph;

public interface NonAtomicFactory<T,D> {
    public NonAtomicObject<T,D> construct(
        String host_uuid, boolean peered, Object init_val);
}
