package RalphDataWrappers;


public class ValueTypeDataWrapperFactory<T,D>
    extends DataWrapperFactory<T,D>
{	
    @Override
    public DataWrapper<T,D> construct(T _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<T,D>(_val,log_changes);
    }
}
