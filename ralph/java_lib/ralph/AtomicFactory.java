package ralph;

/**
 * @param <T> --- The internal, java, type of the data
 */
public interface AtomicFactory<T>
{
    public AtomicObject<T> construct(
        String host_uuid, boolean log_changes, Object init_val);
}
