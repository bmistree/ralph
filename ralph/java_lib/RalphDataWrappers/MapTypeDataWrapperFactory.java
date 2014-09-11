package RalphDataWrappers;

import java.util.Map;
import ralph.RalphObject;
import ralph.ActiveEvent;

/**
 * @param <K> --- Key for the map
 * @param <V> --- Java variables in the hashmap
 */

public class MapTypeDataWrapperFactory<KeyType,ValueType,ValueDeltaType>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    Map<KeyType,RalphObject<ValueType,ValueDeltaType>>
    >
{
    private final Class<KeyType> key_type_class;
    private final Class<ValueType> value_type_class;

    public MapTypeDataWrapperFactory(
        Class<KeyType> key_type_class,
        Class<ValueType> value_type_class)
    {
        this.key_type_class = key_type_class;
        this.value_type_class = value_type_class;
    }
    
    @Override
    public
        // return type
        DataWrapper<
            Map<KeyType, RalphObject<ValueType,ValueDeltaType>>>
        // method name
        construct(
            Map<KeyType, RalphObject<ValueType,ValueDeltaType>> _val,
            boolean log_changes) 
    {
        return
            new MapTypeDataWrapper<KeyType,ValueType,ValueDeltaType>(
                _val,key_type_class,value_type_class,log_changes);
    }
	
}

