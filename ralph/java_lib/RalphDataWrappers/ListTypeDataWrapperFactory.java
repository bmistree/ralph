package RalphDataWrappers;

import java.util.List;
import ralph.RalphObject;
/**
 * @param <ValueType> --- Java variables in the array list
 */

public class ListTypeDataWrapperFactory<ValueType,ValueDeltaType>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    List<RalphObject<ValueType,ValueDeltaType>>>
    
{
    final public Class<ValueType> value_type_class;
    public ListTypeDataWrapperFactory(
        Class<ValueType> _value_type_class)
    {
        value_type_class = _value_type_class;
    }
    
    @Override
    public DataWrapper<List<RalphObject<ValueType,ValueDeltaType>>>
        construct(
            List<RalphObject<ValueType,ValueDeltaType>> _val,
            boolean log_changes) 
    {
        return
            new ListTypeDataWrapper<ValueType,ValueDeltaType>(
                _val,value_type_class,log_changes);
    }
}

