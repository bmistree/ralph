package RalphDataWrappers;

import java.util.Map;
import ralph.RalphObject;
import ralph.ActiveEvent;

/**
 * @param <K> --- Key for the map
 * @param <V> --- Java variables in the hashmap
 */

public class MapTypeDataWrapperFactory<K,V,ValueDeltaType>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    Map<K,RalphObject<V,ValueDeltaType>>
    >
{

    @Override
    public DataWrapper<Map<K, RalphObject<V,ValueDeltaType>>>
        construct(
            Map<K, RalphObject<V,ValueDeltaType>> _val, boolean log_changes) 
    {
        return new MapTypeDataWrapper<K,V,ValueDeltaType>(_val,log_changes);
    }
	
}

