package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import java.io.IOException;

import ralph_protobuffs.VariablesProto;
import ralph_protobuffs.UtilProto.UUID;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;

import RalphDataWrappers.NumberTypeDataWrapperFactory;
import RalphDataWrappers.TextTypeDataWrapperFactory;
import RalphDataWrappers.TrueFalseTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ServiceFactoryTypeDataWrapperFactory;
import RalphDataWrappers.ServiceReferenceTypeDataWrapperFactory;


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

    final static ServiceReferenceTypeDataWrapperFactory
        service_reference_value_type_data_wrapper_factory = 
        new ServiceReferenceTypeDataWrapperFactory();

    final static InternalServiceReference default_service_reference = null;

    /** Atomics */
    
    public static class AtomicNumberVariable
        extends AtomicValueVariable<Double>
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
        protected SpeculativeAtomicObject<Double>
            duplicate_for_speculation(Double to_speculate_on)
        {
            SpeculativeAtomicObject<Double> to_return =
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
        
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder) throws BackoutException
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class AtomicTextVariable
        extends AtomicValueVariable<String>
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
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
        
        @Override
        protected SpeculativeAtomicObject<String>
            duplicate_for_speculation(String to_speculate_on)
        {
            SpeculativeAtomicObject<String> to_return =
                new AtomicTextVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
    }
	
    public static class AtomicTrueFalseVariable
        extends AtomicValueVariable<Boolean>
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
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
        
        @Override
        protected SpeculativeAtomicObject<Boolean>
            duplicate_for_speculation(Boolean to_speculate_on)
        {
            SpeculativeAtomicObject<Boolean> to_return =
                new AtomicTrueFalseVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
    }

    public static class AtomicEnumVariable<T>
        extends AtomicValueVariable<T>
    {
        public AtomicEnumVariable(
            boolean _log_changes,T init_val, RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                new ValueTypeDataWrapperFactory<T>(),
                ralph_globals);
        }
        public AtomicEnumVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,null,
                new ValueTypeDataWrapperFactory<T>(),
                ralph_globals);
        }

        @Override
        protected SpeculativeAtomicObject<T>
            duplicate_for_speculation(T to_speculate_on)
        {
            SpeculativeAtomicObject<T> to_return =
                new AtomicEnumVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            // FIXME: Should allow serializing enum types across
            // network.
            Util.logger_assert(
                "FIXME: Should allow serializing enums across network.");
        }
    }
    
    public static class AtomicInterfaceVariable<T>
        extends AtomicValueVariable<T>
    {
        public AtomicInterfaceVariable(
            boolean _log_changes,T init_val, RalphGlobals ralph_globals)
        {
            super(
                _log_changes,init_val,
                new ValueTypeDataWrapperFactory<T>(),
                ralph_globals);
        }
        public AtomicInterfaceVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,null,
                new ValueTypeDataWrapperFactory<T>(),
                ralph_globals);
        }

        @Override
        protected SpeculativeAtomicObject<T>
            duplicate_for_speculation(T to_speculate_on)
        {
            SpeculativeAtomicObject<T> to_return =
                new AtomicInterfaceVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            Util.logger_assert(
                "Cannot pass reference to interface across network.");
        }
    }
    
    public static class AtomicServiceFactoryVariable
        extends AtomicValueVariable<InternalServiceFactory>
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
        protected SpeculativeAtomicObject<InternalServiceFactory>
            duplicate_for_speculation(InternalServiceFactory to_speculate_on)
        {
            SpeculativeAtomicObject<InternalServiceFactory> to_return =
                new AtomicServiceFactoryVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            serialize_service_factory(internal_val,any_builder);
            any_builder.setIsTvar(true);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }

        /**
           Can be used by both atomic and non-atomic service factories.
         */
        public static void serialize_service_factory (
            InternalServiceFactory internal_val,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            any_builder.setVarName("");
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
    
    public static class AtomicServiceReferenceVariable
        extends AtomicValueVariable<InternalServiceReference>
    {
        public AtomicServiceReferenceVariable(
            boolean _log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,(InternalServiceReference)init_val,
                service_reference_value_type_data_wrapper_factory,ralph_globals);
        }
        public AtomicServiceReferenceVariable(
            boolean _log_changes,RalphGlobals ralph_globals)
        {
            super(
                _log_changes,default_service_reference,
                service_reference_value_type_data_wrapper_factory,ralph_globals);
        }

        @Override
        protected SpeculativeAtomicObject<InternalServiceReference>
            duplicate_for_speculation(InternalServiceReference to_speculate_on)
        {
            SpeculativeAtomicObject<InternalServiceReference> to_return =
                new AtomicServiceReferenceVariable(
                    log_changes,to_speculate_on,ralph_globals);
            to_return.set_derived(this);
            return to_return;
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceReference internal_val = get_val(active_event);
            serialize_service_reference(internal_val,any_builder);
            any_builder.setIsTvar(true);
        }

        
        /**
           Can be used by both atomic and non-atomic service factories.
         */
        public static void serialize_service_reference (
            InternalServiceReference internal_val,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            any_builder.setVarName("");

            UUID.Builder service_uuid = UUID.newBuilder();
            service_uuid.setData(internal_val.service_uuid);
            
            VariablesProto.Variables.ServiceReference.Builder service_reference =
                VariablesProto.Variables.ServiceReference.newBuilder();
            
            service_reference.setIpAddr(internal_val.ip_addr);
            service_reference.setTcpPort(internal_val.tcp_port);
            service_reference.setServiceUuid(service_uuid);

            any_builder.setServiceReference(service_reference);
        }
        
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }

    }
    
    public static class NonAtomicNumberVariable
        extends NonAtomicValueVariable<Double>
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
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class NonAtomicTextVariable
        extends NonAtomicValueVariable<String>
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
        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class NonAtomicTrueFalseVariable
        extends NonAtomicValueVariable<Boolean>
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
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setIsTvar(false);
        }

        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    public static class NonAtomicEnumVariable<T>
        extends NonAtomicValueVariable<T>
    {
        public NonAtomicEnumVariable(
            boolean _dummy_log_changes, T init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                new ValueTypeDataWrapperFactory<T>(),
                ralph_globals);
        }

        public NonAtomicEnumVariable(
            boolean _dummy_log_changes, RalphGlobals ralph_globals)
        {
            super(
                null,new ValueTypeDataWrapperFactory<T>(),ralph_globals);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            // FIXME: Should allow serializing enums across network.
            any_builder.setIsTvar(false);
            Util.logger_assert(
                "FIXME: Should allow serializing enum variables across network");
        }
    }
    
    public static class NonAtomicInterfaceVariable<T>
        extends NonAtomicValueVariable<T>
    {
        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, T init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                new ValueTypeDataWrapperFactory<T>(),
                ralph_globals);
        }

        public NonAtomicInterfaceVariable(
            boolean _dummy_log_changes, RalphGlobals ralph_globals)
        {
            super(
                null,new ValueTypeDataWrapperFactory<T>(),ralph_globals);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
        {
            any_builder.setIsTvar(false);
            Util.logger_assert(
                "Cannot pass interface reference over network.");
        }
    }
    
    public static class NonAtomicServiceFactoryVariable
        extends NonAtomicValueVariable<InternalServiceFactory>
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
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            AtomicServiceFactoryVariable.serialize_service_factory(
                internal_val,any_builder);
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }


    public static class NonAtomicServiceReferenceVariable
        extends NonAtomicValueVariable<InternalServiceReference>
    {
        public NonAtomicServiceReferenceVariable(
            boolean _log_changes,InternalServiceReference init_val,
            RalphGlobals ralph_globals)
        {
            super(
                init_val,
                service_reference_value_type_data_wrapper_factory,
                ralph_globals);
        }
        public NonAtomicServiceReferenceVariable(
            boolean _dummy_log_changes,RalphGlobals ralph_globals)
        {
            super(
                default_service_reference,
                service_reference_value_type_data_wrapper_factory,
                ralph_globals);
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,
            VariablesProto.Variables.Any.Builder any_builder)
            throws BackoutException
        {
            InternalServiceReference internal_val = get_val(active_event);
            AtomicServiceReferenceVariable.serialize_service_reference(
                internal_val,any_builder);
            any_builder.setIsTvar(false);
        }
        @Override
        public void deserialize_rpc(
            RalphGlobals ralph_globals, VariablesProto.Variables.Any any)
        {
            Util.logger_assert(
                "Should deserialize directly in DataConstructorRegistry.");
        }
    }

    
        
    /************ Handling maps ********/
    //non-atomic
    public static class NonAtomicMapVariable <K,V>
        extends NonAtomicMap<K,V>
    {
        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_index_type,locked_wrapper,ralph_globals);
        }

        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap<K,V> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(internal_val,_index_type,locked_wrapper,ralph_globals);
        }
    }

    // atomic
    public static class AtomicMapVariable <K,V>
        extends AtomicMap<K,V>
    {
        public AtomicMapVariable(
            boolean _log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_log_changes,_index_type,locked_wrapper,ralph_globals);
        }

        public AtomicMapVariable(
            boolean _log_changes,
            AtomicInternalMap<K,V> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(
                _log_changes,internal_val,_index_type,locked_wrapper,
                ralph_globals);
        }        
    }
    
    /************ Handling Lists ********/
    public static class AtomicListVariable <V>
        extends AtomicList<V>
    {
        public AtomicListVariable(
            boolean _log_changes,EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_log_changes,locked_wrapper,ralph_globals);
        }


        public AtomicListVariable(
            boolean _log_changes,AtomicInternalList<V> internal_val,
            EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(_log_changes,internal_val,locked_wrapper,ralph_globals);
        }
    }

    public static class NonAtomicListVariable <V>
        extends NonAtomicList<V>
    {
        public NonAtomicListVariable(
            boolean _dummy_log_changes, EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(locked_wrapper,ralph_globals);
        }

        public NonAtomicListVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalList<V> internal_val,
            EnsureAtomicWrapper<V> locked_wrapper,
            RalphGlobals ralph_globals)
        {
            super(internal_val,locked_wrapper,ralph_globals);
        }
    }
}
