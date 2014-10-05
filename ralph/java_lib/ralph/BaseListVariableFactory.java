package ralph;

import ralph.Variables.AtomicListVariable;
import ralph.Variables.NonAtomicListVariable;

import RalphAtomicWrappers.EnsureAtomicWrapper;
import static RalphAtomicWrappers.BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER;

import static RalphAtomicWrappers.BaseAtomicWrappers.ATOMIC_NUMBER_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.ATOMIC_TEXT_WRAPPER;
import static RalphAtomicWrappers.BaseAtomicWrappers.ATOMIC_TRUE_FALSE_WRAPPER;

public class BaseListVariableFactory
{
    // Java uses a lazy class loader and lazy initialization.  What
    // this means is that side effect of registering all these
    // factories with containerfactory may not happen, unless code
    // that needs them to be registered first touches a field in this
    // class.
    public final static BaseListVariableFactory instance =
        new BaseListVariableFactory();
    public void force_initialization(){}

    private BaseListVariableFactory()
    {}

    public final static ListVariableFactory<Double,Double>
        double_factory = new ListVariableFactory(
            Double.class,ATOMIC_NUMBER_WRAPPER);
    public final static ListVariableFactory<Double,Double>
        string_factory = new ListVariableFactory(
            String.class,ATOMIC_TEXT_WRAPPER);
    public final static ListVariableFactory<Boolean,Boolean>
        boolean_factory = new ListVariableFactory(
            Boolean.class,ATOMIC_TRUE_FALSE_WRAPPER);    

    
    public static class ListVariableFactory<ValueType,ValueDeltaType>
        implements IListVariableFactory
    {
        private final Class <ValueType> value_type_class;
        private final EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper;
        
        public ListVariableFactory(
            Class <ValueType> _value_type_class,
            EnsureAtomicWrapper<ValueType,ValueDeltaType> _locked_wrapper)
        {
            value_type_class = _value_type_class;
            locked_wrapper = _locked_wrapper;
            ContainerFactorySingleton.instance.add_atomic_list_variable_factory(
                value_type_class.getName(),this);
        }
        
        @Override
        public AtomicListVariable construct_atomic(RalphGlobals ralph_globals)
        {
            return new Variables.AtomicListVariable<ValueType,ValueDeltaType>(
                false,locked_wrapper,value_type_class,ralph_globals);
        }

        @Override
        public NonAtomicListVariable construct_non_atomic(RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicListVariable<ValueType,ValueDeltaType>(
                false,locked_wrapper,value_type_class,ralph_globals);
        }
    }
}