package RalphDataWrappers;

public class ValueTypeDataWrapperFactory<T>
    extends DataWrapperFactory<T>
{	
    @Override
    public DataWrapper<T> construct(T _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<T>(_val,log_changes);
    }
}
