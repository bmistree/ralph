package RalphDataWrappers;

import java.util.HashMap;
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
 * @param <D> --- What the java variables in the hashmap should
 * dewaldoify into (if they are locked objects)
 */

public class MapTypeDataWrapperFactory<K,V,D> 	
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    HashMap<K,RalphObject<V,D>>, 
    // what you get when you call dewaldoify on the data wrapper
    HashMap<K,D> >
{

    @Override
    public DataWrapper<HashMap<K, RalphObject<V,D>>, HashMap<K,D>>
        construct(
            HashMap<K, RalphObject<V,D>> _val, boolean log_changes) 
    {
        return new MapTypeDataWrapper<K,V,D>(_val,log_changes);
    }
	
}

