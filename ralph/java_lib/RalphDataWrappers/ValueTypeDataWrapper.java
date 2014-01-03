package RalphDataWrappers;

public class ValueTypeDataWrapper<T,D> extends DataWrapper<T,D>
{
    public ValueTypeDataWrapper(T _val, boolean log_changes)
    {
        super(_val, log_changes);
    }
}
