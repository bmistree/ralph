package RalphDataWrappers;

public class TextTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<String,String>
{	
    @Override
    public DataWrapper<String,String> construct(
        String val, boolean log_changes)
    {
        String new_val = null;
        if (val != null)
            new_val = new String(val);
        
        return new ValueTypeDataWrapper<String,String>(
            new_val,log_changes);
    }
}
