package RalphDataWrappers;

import java.util.List;
import ralph.RalphObject;
/**
 * @param <V> --- Java variables in the array list
 */

public class ListTypeDataWrapperFactory<V,ValueDeltaType>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    List<RalphObject<V,ValueDeltaType>>>
    
{
    @Override
    public DataWrapper<List<RalphObject<V,ValueDeltaType>>>
        construct(
            List<RalphObject<V,ValueDeltaType>> _val, boolean log_changes) 
    {
        return new ListTypeDataWrapper<V,ValueDeltaType>(_val,log_changes);
    }
}

