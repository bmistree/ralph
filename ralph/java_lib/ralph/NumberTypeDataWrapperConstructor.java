package ralph;

public class NumberTypeDataWrapperConstructor
    extends ValueTypeDataWrapperConstructor<Double,Double>
{	
    @Override
    public DataWrapper<Double,Double> construct(
        Double _val, boolean peered)
    {
        return new ValueTypeDataWrapper<Double,Double>(
            new Double(_val),peered);
    }
}
