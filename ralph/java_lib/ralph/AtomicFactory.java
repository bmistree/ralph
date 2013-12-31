package ralph;

/**
 * 
 * @author bmistree
 *
 * @param <T> --- The internal, java, type of the data
 * @param <D> --- The java type of the data that is returned when call dewaldoify
 */
public interface AtomicFactory<T,D>
{
    public AtomicObject<T,D> construct(
        String host_uuid, boolean peered, Object init_val);
}
