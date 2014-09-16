package ralph;

import ralph.Variables.AtomicMapVariable;
import ralph.NonAtomicInternalMap.IndexType;

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


public class BaseAtomicMapVariableFactory
{
    // can keep this private because side effect is that adding to
    // ContainerFactorySingleton.
    private final static AtomicMapVariableFactory<Double,Double,Double>
        double_double_factory = new AtomicMapVariableFactory(
            Double.class,Double.class,IndexType.DOUBLE,
            ATOMIC_NUMBER_WRAPPER,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    private final static AtomicMapVariableFactory<Double,String,String>
        double_string_factory = new AtomicMapVariableFactory(
            Double.class,String.class,IndexType.DOUBLE,
            ATOMIC_TEXT_WRAPPER,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    private final static AtomicMapVariableFactory<Double,Boolean,Boolean>
        double_boolean_factory = new AtomicMapVariableFactory(
            Double.class,Boolean.class,IndexType.DOUBLE,
            ATOMIC_TRUE_FALSE_WRAPPER,
            DOUBLE_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);


    // String keyed
    private final static AtomicMapVariableFactory<String,Double,Double>
        string_double_factory = new AtomicMapVariableFactory(
            String.class,Double.class,IndexType.STRING,
            ATOMIC_NUMBER_WRAPPER,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    private final static AtomicMapVariableFactory<String,String,String>
        string_string_factory = new AtomicMapVariableFactory(
            String.class,String.class,IndexType.STRING,
            ATOMIC_TEXT_WRAPPER,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    private final static AtomicMapVariableFactory<String,Boolean,Boolean>
        string_boolean_factory = new AtomicMapVariableFactory(
            String.class,Boolean.class,IndexType.STRING,
            ATOMIC_TRUE_FALSE_WRAPPER,
            STRING_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    // Boolean keyed
    private final static AtomicMapVariableFactory<Boolean,Double,Double>
        boolean_double_factory = new AtomicMapVariableFactory(
            String.class,Double.class,IndexType.BOOLEAN,
            ATOMIC_NUMBER_WRAPPER,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);
    
    private final static AtomicMapVariableFactory<Boolean,String,String>
        boolean_string_factory = new AtomicMapVariableFactory(
            String.class,String.class,IndexType.BOOLEAN,
            ATOMIC_TEXT_WRAPPER,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);

    private final static AtomicMapVariableFactory<Boolean,Boolean,Boolean>
        boolean_boolean_factory = new AtomicMapVariableFactory(
            String.class,Boolean.class,IndexType.BOOLEAN,
            ATOMIC_TRUE_FALSE_WRAPPER,
            BOOLEAN_KEYED_INTERNAL_MAP_TYPE_VERSION_HELPER);
    
    private static class AtomicMapVariableFactory<KeyType,ValueType,ValueDeltaType>
        implements IAtomicMapVariableFactory
    {
        private final Class <KeyType> key_type_class;
        private final Class <ValueType> value_type_class;
        private final NonAtomicInternalMap.IndexType index_type;
        private final EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper;
        private final InternalContainerTypeVersionHelper<KeyType> internal_version_helper;
        
        public AtomicMapVariableFactory(
            Class <KeyType> _key_type_class, Class <ValueType> _value_type_class,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> _locked_wrapper,
            InternalContainerTypeVersionHelper<KeyType> _internal_version_helper)
        {
            key_type_class = _key_type_class;
            value_type_class = _value_type_class;
            index_type = _index_type;
            locked_wrapper = _locked_wrapper;
            internal_version_helper = _internal_version_helper;
            ContainerFactorySingleton.instance.add_atomic_map_variable_factory(
                key_type_class.getName(),value_type_class.getName(),this);
        }
        
        @Override
        public AtomicMapVariable construct(RalphGlobals ralph_globals)
        {
            return new Variables.AtomicMapVariable<KeyType,ValueType,ValueDeltaType>(
                false,index_type,locked_wrapper,internal_version_helper,
                key_type_class,value_type_class,ralph_globals);
        }
    }
}