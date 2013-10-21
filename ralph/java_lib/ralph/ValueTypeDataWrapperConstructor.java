package ralph;


public class ValueTypeDataWrapperConstructor<T,D>
    extends DataWrapperConstructor<T,D>
{	
    @Override
    public DataWrapper<T,D> construct(T _val, boolean peered)
    {
        return new ValueTypeDataWrapper<T,D>(_val,peered);
    }
}
