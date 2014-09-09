package RalphDataWrappers;

public class TextTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<String>
{	
    @Override
    public DataWrapper<String> construct(
        String val, boolean log_changes)
    {
        String new_val = null;
        if (val != null)
            new_val = new String(val);
        
        return new ValueTypeDataWrapper<String>(
            new_val,log_changes);
    }
}
