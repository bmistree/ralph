package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;

import RalphDataWrappers.NumberTypeDataWrapperFactory;
import RalphDataWrappers.TextTypeDataWrapperFactory;
import RalphDataWrappers.TrueFalseTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ServiceFactoryTypeDataWrapperFactory;

public class Variables {
    public final static NumberTypeDataWrapperFactory
        number_value_type_data_wrapper_factory =
        new NumberTypeDataWrapperFactory();
    final static Double default_number = new Double(0.0);

    final static TextTypeDataWrapperFactory
        text_value_type_data_wrapper_factory =
        new TextTypeDataWrapperFactory();
    
    final static String default_text = new String();
	
    final static TrueFalseTypeDataWrapperFactory
        true_false_value_type_data_wrapper_factory = 
        new TrueFalseTypeDataWrapperFactory();
    
    final static Boolean default_tf = false;
	
    final static ServiceFactoryTypeDataWrapperFactory
        service_factory_value_type_data_wrapper_factory = 
        new ServiceFactoryTypeDataWrapperFactory();

    final static InternalServiceFactory default_service_factory = null;


    /** Atomics */
    
    public static class AtomicNumberVariable
        extends AtomicValueVariable<Double,Double>
    {
        public AtomicNumberVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,
                new Double(((Number) init_val).doubleValue()),
                number_value_type_data_wrapper_factory,
                ralph_globals);
        }
        
        @Override
        protected SpeculativeAtomicObject<Double,Double>
            duplicate_for_speculation(Double to_speculate_on)
        {
            SpeculativeAtomicObject<Double,Double> to_return =
                new AtomicNumberVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        
        public AtomicNumberVariable(boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_number,
                number_value_type_data_wrapper_factory,ralph_globals);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setReference(is_reference);
            any_builder.setIsTvar(true);
        }
    }

    public static class AtomicTextVariable
        extends AtomicValueVariable<String,String>
    {
        public AtomicTextVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,(String)init_val,
                text_value_type_data_wrapper_factory,ralph_globals);
        }
        public AtomicTextVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_text,
                text_value_type_data_wrapper_factory,ralph_globals);
        }
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setReference(is_reference);
            any_builder.setIsTvar(true);
        }
        
        @Override
        protected SpeculativeAtomicObject<String,String>
            duplicate_for_speculation(String to_speculate_on)
        {
            SpeculativeAtomicObject<String,String> to_return =
                new AtomicTextVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
    }
	
    public static class AtomicTrueFalseVariable
        extends AtomicValueVariable<Boolean,Boolean>
    {
        public AtomicTrueFalseVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,(Boolean)init_val,
                true_false_value_type_data_wrapper_factory,ralph_globals);
        }
        public AtomicTrueFalseVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_tf,
                true_false_value_type_data_wrapper_factory,ralph_globals);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setReference(is_reference);
            any_builder.setIsTvar(true);
        }
        
        @Override
        protected SpeculativeAtomicObject<Boolean,Boolean>
            duplicate_for_speculation(Boolean to_speculate_on)
        {
            SpeculativeAtomicObject<Boolean,Boolean> to_return =
                new AtomicTrueFalseVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
    }

    public static class AtomicInterfaceVariable<T>
        extends AtomicValueVariable<T,T>
    {
        public AtomicInterfaceVariable(
            boolean _log_changes,T init_val, RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                new ValueTypeDataWrapperFactory<T,T>(),
                ralph_globals);
        }
        public AtomicInterfaceVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,null,
                new ValueTypeDataWrapperFactory<T,T>(),
                ralph_globals);
        }

        @Override
        protected SpeculativeAtomicObject<T,T>
            duplicate_for_speculation(T to_speculate_on)
        {
            SpeculativeAtomicObject<T,T> to_return =
                new AtomicInterfaceVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Util.logger_assert(
                "Cannot pass reference to interface across network.");
        }
    }
    
    public static class AtomicServiceFactoryVariable
        extends AtomicValueVariable<InternalServiceFactory,InternalServiceFactory>
    {
        public AtomicServiceFactoryVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,(InternalServiceFactory)init_val,
                service_factory_value_type_data_wrapper_factory,ralph_globals);
        }
        public AtomicServiceFactoryVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_service_factory,
                service_factory_value_type_data_wrapper_factory,ralph_globals);
        }

        @Override
        protected SpeculativeAtomicObject<InternalServiceFactory,InternalServiceFactory>
            duplicate_for_speculation(InternalServiceFactory to_speculate_on)
        {
            SpeculativeAtomicObject<InternalServiceFactory,InternalServiceFactory> to_return =
                new AtomicServiceFactoryVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }

        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            serialize_service_factory(internal_val,any_builder,is_reference);
            any_builder.setIsTvar(true);
        }

        /**
           Can be used by both atomic and non-atomic service factories.
         */
        public static void serialize_service_factory (
            InternalServiceFactory internal_val,
            VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            any_builder.setVarName("");
            any_builder.setReference(is_reference);
            try {
                any_builder.setServiceFactory(
                    com.google.protobuf.ByteString.copyFrom(
                        internal_val.convert_constructor_to_byte_array()));
            } catch (IOException ex) {
                ex.printStackTrace();
                Util.logger_assert(
                    "\nUnhandled IOException when serializing " +
                    "service factory.\n");
            }
        }
    }
    
    
    public static class NonAtomicNumberVariable
        extends NonAtomicValueVariable<Double,Double>
    {
        public NonAtomicNumberVariable(
            boolean _dummy_log_changes, Double init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                number_value_type_data_wrapper_factory,
                ralph_globals);
        }

        public NonAtomicNumberVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_number,
                number_value_type_data_wrapper_factory,ralph_globals);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setReference(is_reference);
            any_builder.setIsTvar(false);
        }
    }

    public static class NonAtomicTextVariable
        extends NonAtomicValueVariable<String,String>
    {
        public NonAtomicTextVariable(
            boolean _dummy_log_changes,String init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                text_value_type_data_wrapper_factory,ralph_globals);
        }
		
        public NonAtomicTextVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_text,
                text_value_type_data_wrapper_factory,ralph_globals);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setReference(is_reference);
            any_builder.setIsTvar(false);
        }
        
    }

    public static class NonAtomicTrueFalseVariable
        extends NonAtomicValueVariable<Boolean,Boolean>
    {
        public NonAtomicTrueFalseVariable(
            boolean _dummy_log_changes, Boolean init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                true_false_value_type_data_wrapper_factory,ralph_globals);
        }

        public NonAtomicTrueFalseVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_tf,
                true_false_value_type_data_wrapper_factory,ralph_globals);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setReference(is_reference);
            any_builder.setIsTvar(false);
        }
    }

    public static class NonAtomicInterfaceVariable<T>
        extends NonAtomicValueVariable<T,T>
    {
        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, T init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                new ValueTypeDataWrapperFactory<T,T>(),
                ralph_globals);
        }

        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, RalphGlobals ralph_globals)
        {
            super(
                null,new ValueTypeDataWrapperFactory<T,T>(),ralph_globals);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            any_builder.setIsTvar(false);
            Util.logger_assert(
                "Cannot pass interface reference over network.");
        }
    }
    
    public static class NonAtomicServiceFactoryVariable
        extends NonAtomicValueVariable<InternalServiceFactory,InternalServiceFactory>
    {
        public NonAtomicServiceFactoryVariable(
            boolean _log_changes,InternalServiceFactory init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                service_factory_value_type_data_wrapper_factory,
                ralph_globals);
        }
        public NonAtomicServiceFactoryVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_service_factory,
                service_factory_value_type_data_wrapper_factory,
                ralph_globals);
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            AtomicServiceFactoryVariable.serialize_service_factory(
                internal_val,any_builder,is_reference);
            any_builder.setIsTvar(false);
        }
    }

        
    /************ Handling maps ********/
    //non-atomic
    public static class NonAtomicMapVariable <K,V,D>
        extends NonAtomicMap<K,V,D>
    {
        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_index_type,locked_wrapper,ralph_globals);
        }

        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap<K,V,D> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(internal_val,_index_type,locked_wrapper,ralph_globals);
        }
    }

    // atomic
    public static class AtomicMapVariable <K,V,D>
        extends AtomicMap<K,V,D>
    {
        public AtomicMapVariable(
            boolean _log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_log_changes,_index_type,locked_wrapper,ralph_globals);
        }

        public AtomicMapVariable(
            boolean _log_changes,
            AtomicInternalMap<K,V,D> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,internal_val,_index_type,locked_wrapper,
                ralph_globals);
        }        
    }
    
    /************ Handling Lists ********/
    public static class AtomicListVariable <V,D>
        extends AtomicList<V,D>
    {
        public AtomicListVariable(
            boolean _log_changes,EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_log_changes,locked_wrapper,ralph_globals);
        }


        public AtomicListVariable(
            boolean _log_changes,AtomicInternalList<V,D> internal_val,
            EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_log_changes,internal_val,locked_wrapper,ralph_globals);
        }
    }

    public static class NonAtomicListVariable <V,D>
        extends NonAtomicList<V,D>
    {
        public NonAtomicListVariable(
            boolean _dummy_log_changes, EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(locked_wrapper,ralph_globals);
        }

        public NonAtomicListVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalList<V,D> internal_val,
            EnsureAtomicWrapper<V,D> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(internal_val,locked_wrapper,ralph_globals);
        }
    }
}
