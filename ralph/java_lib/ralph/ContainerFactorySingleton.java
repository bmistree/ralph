package ralph;

import java.util.Map;
import java.util.HashMap;

public class ContainerFactorySingleton
{
    public final static ContainerFactorySingleton instance =
        new ContainerFactorySingleton();

    // first string is the key's class name, second key is the value's
    // class name.
    private final Map<String,
        Map <String, IMapVariableFactory>> atomic_map_factories =
            new HashMap<String,
                Map <String, IMapVariableFactory>> ();

    // key is value's class name
    private final Map<String,IListVariableFactory>
        atomic_list_factories =
            new HashMap<String,IListVariableFactory>();

    // key is class name of struct
    private final Map<String, IAtomicStructWrapperBaseClassFactory>
        atomic_struct_factories =
            new HashMap<String,IAtomicStructWrapperBaseClassFactory>();
    
    
    private ContainerFactorySingleton()
    {}

    
    public IMapVariableFactory get_atomic_map_variable_factory(
        String key_class_name,String val_class_name)
    {
        if (atomic_map_factories.containsKey(key_class_name) &&
             (atomic_map_factories.get(key_class_name).containsKey(val_class_name)))
        {
            return atomic_map_factories.get(key_class_name).get(val_class_name);
        }
        return null;
    }

    public IListVariableFactory get_list_variable_factory(
        String val_class_name)
    {
        return atomic_list_factories.get(val_class_name);
    }

    public IAtomicStructWrapperBaseClassFactory
        get_atomic_struct_wrapper_base_class_factory(
            String val_class_name)
    {
        return atomic_struct_factories.get(val_class_name);
    }
    
    public void add_atomic_map_variable_factory(
        String key_class_name,String val_class_name,IMapVariableFactory factory)
    {
        if (! atomic_map_factories.containsKey(key_class_name))
        {
            atomic_map_factories.put(
                key_class_name,new HashMap<String,IMapVariableFactory>());
        }
        atomic_map_factories.get(key_class_name).put(val_class_name,factory);
    }

    public void add_atomic_list_variable_factory(
        String val_class_name,IListVariableFactory factory)
    {
        atomic_list_factories.put(val_class_name,factory);
    }
    
    public void add_atomic_struct_wrapper_base_class_factory(
        String val_class_name,IAtomicStructWrapperBaseClassFactory factory)
    {
        atomic_struct_factories.put(val_class_name,factory);
    }
}