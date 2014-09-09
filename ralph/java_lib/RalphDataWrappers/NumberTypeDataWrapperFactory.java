package RalphDataWrappers;
import ralph.RalphObject;

public class NumberTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<Double>
{	
    @Override
    public DataWrapper<Double> construct(
        Double val, boolean log_changes)
    {
        Double new_val = null;
        if (val != null)
            new_val = new Double(val);
        return new ValueTypeDataWrapper<Double>(new_val,log_changes);
    }
}
