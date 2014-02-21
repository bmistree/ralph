package RalphDataWrappers;

public class TrueFalseTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<Boolean,Boolean>
{	
    @Override
    public DataWrapper<Boolean,Boolean> construct(
        Boolean val, boolean log_changes)
    {
        Boolean new_val = null;
        if (val != null)
            new_val = new Boolean(val);
        
        return new ValueTypeDataWrapper<Boolean,Boolean>(
            new_val,log_changes);
    }
}
