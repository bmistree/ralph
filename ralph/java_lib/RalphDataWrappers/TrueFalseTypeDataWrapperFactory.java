package RalphDataWrappers;

public class TrueFalseTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<Boolean,Boolean>
{	
    @Override
    public DataWrapper<Boolean,Boolean> construct(
        Boolean _val, boolean peered)
    {
        return new ValueTypeDataWrapper<Boolean,Boolean>(
            new Boolean(_val),peered);
    }
}
