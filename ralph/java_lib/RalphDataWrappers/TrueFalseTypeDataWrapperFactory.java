package RalphDataWrappers;

public class TrueFalseTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<Boolean,Boolean>
{	
    @Override
    public DataWrapper<Boolean,Boolean> construct(
        Boolean _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<Boolean,Boolean>(
            new Boolean(_val),log_changes);
    }
}
