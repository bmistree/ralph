package RalphDataWrappers;


public class ValueTypeDataWrapperFactory<T,D>
    extends DataWrapperFactory<T,D>
{	
    @Override
    public DataWrapper<T,D> construct(T _val, boolean peered)
    {
        return new ValueTypeDataWrapper<T,D>(_val,peered);
    }
}
