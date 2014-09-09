package RalphDataWrappers;

/**
 * @param <T> --- The type of the internal data
 */
public abstract class DataWrapperFactory<T>
{
    public abstract DataWrapper<T> construct(T _val, boolean log_changes);
}
