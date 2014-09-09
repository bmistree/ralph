package RalphDataWrappers;

public class ValueTypeDataWrapper<T> extends DataWrapper<T>
{
    public ValueTypeDataWrapper(T _val, boolean log_changes)
    {
        super(_val, log_changes);
    }
}
