package ralph;

public class TextTypeDataWrapperConstructor
    extends ValueTypeDataWrapperConstructor<String,String>
{	
    @Override
    public DataWrapper<String,String> construct(
        String _val, boolean peered)
    {
        return new ValueTypeDataWrapper<String,String>(
            new String(_val),peered);
    }
}
