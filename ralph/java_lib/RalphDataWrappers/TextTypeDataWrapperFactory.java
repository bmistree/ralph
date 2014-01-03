package RalphDataWrappers;

public class TextTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<String,String>
{	
    @Override
    public DataWrapper<String,String> construct(
        String _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<String,String>(
            new String(_val),log_changes);
    }
}
