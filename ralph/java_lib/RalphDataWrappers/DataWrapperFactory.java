package RalphDataWrappers;

/**
 * @param <S> --- The type of the internal data
 * @param <T> --- The type that gets returned from de waldoify
 */
public abstract class DataWrapperFactory<S,T>
{
    public abstract DataWrapper<S,T> construct(S _val, boolean log_changes);
}
