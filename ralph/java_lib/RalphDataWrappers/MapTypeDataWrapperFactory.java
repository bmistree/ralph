package RalphDataWrappers;

import java.util.Map;
import ralph.RalphObject;
import ralph.ActiveEvent;

/**
 * 
 * @author bmistree
 *
 * key, value, returned from dewaldoify
 *
 * @param <K> --- Key for the map
 * @param <V> --- Java variables in the hashmap
 */

public class MapTypeDataWrapperFactory<K,V>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    Map<K,RalphObject<V>>
    >
{

    @Override
    public DataWrapper<Map<K, RalphObject<V>>>
        construct(
            Map<K, RalphObject<V>> _val, boolean log_changes) 
    {
        return new MapTypeDataWrapper<K,V>(_val,log_changes);
    }
	
}

