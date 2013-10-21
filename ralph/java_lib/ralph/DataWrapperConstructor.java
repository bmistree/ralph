package ralph;

/**
 * @param <S> --- The type of the internal data
 * @param <T> --- The type that gets returned from de waldoify
 */
public abstract class DataWrapperConstructor<S,T>
{
    public abstract DataWrapper<S,T> construct(S _val, boolean peered);
}
