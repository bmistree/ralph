package ralph;

public class ValueTypeDataWrapper<T,D> extends DataWrapper<T,D>
{
    public ValueTypeDataWrapper(T _val, boolean peered)
    {
        super(_val, peered);
    }
}
