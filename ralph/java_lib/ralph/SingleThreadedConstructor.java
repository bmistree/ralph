package ralph;

public interface SingleThreadedConstructor<T,D> {
    public NonAtomicObject<T,D> construct(
        String host_uuid, boolean peered, Object init_val);
}
