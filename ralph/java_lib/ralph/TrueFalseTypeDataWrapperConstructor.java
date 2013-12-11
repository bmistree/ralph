package ralph;

public class TrueFalseTypeDataWrapperConstructor
    extends ValueTypeDataWrapperConstructor<Boolean,Boolean>
{	
    @Override
    public DataWrapper<Boolean,Boolean> construct(
        Boolean _val, boolean peered)
    {
        return new ValueTypeDataWrapper<Boolean,Boolean>(
            new Boolean(_val),peered);
    }
}
