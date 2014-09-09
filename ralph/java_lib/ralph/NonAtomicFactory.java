package ralph;

public interface NonAtomicFactory<T,DeltaType> {
    public NonAtomicObject<T,DeltaType> construct(
        String host_uuid, boolean peered, Object init_val);
}
