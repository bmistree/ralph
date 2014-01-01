package RalphDataWrappers;
import ralph.RalphObject;

public class NumberTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<Double,Double>
{	
    @Override
    public DataWrapper<Double,Double> construct(
        Double _val, boolean peered)
    {
        return new ValueTypeDataWrapper<Double,Double>(
            new Double(_val),peered);
    }
}
