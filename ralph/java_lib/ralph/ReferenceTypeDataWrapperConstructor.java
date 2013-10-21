package ralph;

import java.util.HashMap;

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

public class ReferenceTypeDataWrapperConstructor<K,V,D> 	
    extends DataWrapperConstructor<
    // The actual internal data that will be held by the data wrapper
    HashMap<K,LockedObject<V,D>>, 
    // what you get when you call dewaldoify on the data wrapper
    HashMap<K,D> >
{

    @Override
    public DataWrapper<HashMap<K, LockedObject<V,D>>, HashMap<K,D>>
        construct(
            HashMap<K, LockedObject<V,D>> _val, boolean peered) 
    {
        return new ReferenceTypeDataWrapper<K,V,D>(_val,peered);
    }
	
}

