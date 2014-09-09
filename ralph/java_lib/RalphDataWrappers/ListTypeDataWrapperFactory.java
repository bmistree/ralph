package RalphDataWrappers;

import java.util.List;
import ralph.RalphObject;
/**
 * value, returned from dewaldoify
 * @param <V> --- Java variables in the array list
 */

public class ListTypeDataWrapperFactory<V>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    List<RalphObject<V>>>
    
{
    @Override
    public DataWrapper<List<RalphObject<V>>>
        construct(
            List<RalphObject<V>> _val, boolean log_changes) 
    {
        return new ListTypeDataWrapper<V>(_val,log_changes);
    }
}

