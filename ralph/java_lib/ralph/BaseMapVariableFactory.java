package ralph;

import ralph.Variables.AtomicMapVariable;
import ralph.Variables.NonAtomicMapVariable;

import RalphAtomicWrappers.EnsureAtomicWrapper;
import static RalphAtomicWrappers.BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER;

import static RalphAtomicWrappers.BaseAtomicWrappers.ATOMIC_NUMBER_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.ATOMIC_TEXT_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.ATOMIC_TRUE_FALSE_WRAPPER;

import static ralph.BaseTypeVersionHelpers.DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER;
import static ralph.BaseTypeVersionHelpers.BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER;


public class BaseMapVariableFactory
{
    // Java uses a lazy class loader and lazy initialization.  What
    // this means is that side effect of registering all these
    // factories with containerfactory may not happen, unless code
    // that needs them to be registered first touches a field in this
    // class.
    public final static BaseMapVariableFactory instance =
        new BaseMapVariableFactory();
    public void force_initialization(){}

    private BaseMapVariableFactory()
    {}

    
    // can keep this private because side effect is that adding to
    // ContainerFactorySingleton.  
    public final static MapVariableFactory<Double,Double,Double>
        double_double_factory = new MapVariableFactory(
            Double.class,Double.class,ATOMIC_NUMBER_WRAPPER,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    public final static MapVariableFactory<Double,String,String>
        double_string_factory = new MapVariableFactory(
            Double.class,String.class,ATOMIC_TEXT_WRAPPER,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    public final static MapVariableFactory<Double,Boolean,Boolean>
        double_boolean_factory = new MapVariableFactory(
            Double.class,Boolean.class,ATOMIC_TRUE_FALSE_WRAPPER,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);


    // String keyed
    public final static MapVariableFactory<String,Double,Double>
        string_double_factory = new MapVariableFactory(
            String.class,Double.class,ATOMIC_NUMBER_WRAPPER,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    public final static MapVariableFactory<String,String,String>
        string_string_factory = new MapVariableFactory(
            String.class,String.class,ATOMIC_TEXT_WRAPPER,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    public final static MapVariableFactory<String,Boolean,Boolean>
        string_boolean_factory = new MapVariableFactory(
            String.class,Boolean.class,ATOMIC_TRUE_FALSE_WRAPPER,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    // Boolean keyed
    public final static MapVariableFactory<Boolean,Double,Double>
        boolean_double_factory = new MapVariableFactory(
            String.class,Double.class,ATOMIC_NUMBER_WRAPPER,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);
    
    public final static MapVariableFactory<Boolean,String,String>
        boolean_string_factory = new MapVariableFactory(
            String.class,String.class,ATOMIC_TEXT_WRAPPER,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    public final static MapVariableFactory<Boolean,Boolean,Boolean>
        boolean_boolean_factory = new MapVariableFactory(
            String.class,Boolean.class,ATOMIC_TRUE_FALSE_WRAPPER,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);
    
    public static class MapVariableFactory<KeyType,ValueType,ValueDeltaType>
        implements IMapVariableFactory
    {
        private final Class <KeyType> key_type_class;
        private final Class <ValueType> value_type_class;
        private final EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper;
        private final InternalContainerTypeVersionHelper<KeyType> internal_version_helper;
        
        public MapVariableFactory(
            Class <KeyType> _key_type_class, Class <ValueType> _value_type_class,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> _locked_wrapper,
            InternalContainerTypeVersionHelper<KeyType> _internal_version_helper)
        {
            key_type_class = _key_type_class;
            value_type_class = _value_type_class;
            locked_wrapper = _locked_wrapper;
            internal_version_helper = _internal_version_helper;
            ContainerFactorySingleton.instance.add_atomic_map_variable_factory(
                key_type_class.getName(),value_type_class.getName(),this);
        }
        
        @Override
        public AtomicMapVariable construct_atomic(RalphGlobals ralph_globals)
        {
            return new Variables.AtomicMapVariable<KeyType,ValueType,ValueDeltaType>(
                false,locked_wrapper,internal_version_helper,
                key_type_class,value_type_class,ralph_globals);
        }

        @Override
        public NonAtomicMapVariable construct_non_atomic(RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicMapVariable<KeyType,ValueType,ValueDeltaType>(
                false,locked_wrapper,internal_version_helper,
                key_type_class,value_type_class,ralph_globals);
        }
    }
}