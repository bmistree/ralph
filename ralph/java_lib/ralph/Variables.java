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
import RalphDataWrappers.EndpointTypeDataWrapperFactory;
import RalphDataWrappers.ServiceFactoryTypeDataWrapperFactory;

public class Variables {
    final static NumberTypeDataWrapperFactory
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
	
    final static EndpointTypeDataWrapperFactory
        endpoint_value_type_data_wrapper_factory = 
        new EndpointTypeDataWrapperFactory();
    
    final static Endpoint default_endpoint = null;


    final static ServiceFactoryTypeDataWrapperFactory
        service_factory_value_type_data_wrapper_factory = 
        new ServiceFactoryTypeDataWrapperFactory();

    final static InternalServiceFactory default_service_factory = null;

    
    
    public static class AtomicNumberVariable
        extends AtomicValueVariable<Double,Double>
    {
        public AtomicNumberVariable(
            boolean _log_changes,Object init_val)
        {
            super(
                _log_changes,
                new Double(((Number) init_val).doubleValue()),
                default_number,number_value_type_data_wrapper_factory);		
        }
        public AtomicNumberVariable(boolean _log_changes)
        {
            super(
                _log_changes,default_number,default_number,
                number_value_type_data_wrapper_factory);		
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setReference(is_reference);
        }
    }

    public static class AtomicTextVariable
        extends AtomicValueVariable<String,String>
    {
        public AtomicTextVariable(boolean _log_changes,Object init_val)
        {
            super(
                _log_changes,(String)init_val,default_text,
                text_value_type_data_wrapper_factory);		
        }
        public AtomicTextVariable(boolean _log_changes)
        {
            super(
                _log_changes,default_text,default_text,
                text_value_type_data_wrapper_factory);            
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
        }
    }
	
    public static class AtomicTrueFalseVariable
        extends AtomicValueVariable<Boolean,Boolean>
    {
        public AtomicTrueFalseVariable(boolean _log_changes,Object init_val)
        {
            super(
                _log_changes,(Boolean)init_val,default_tf,
                true_false_value_type_data_wrapper_factory);		
        }
        public AtomicTrueFalseVariable(boolean _log_changes)
        {
            super(
                _log_changes,default_tf,default_tf,
                true_false_value_type_data_wrapper_factory);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setReference(is_reference);
        }
    }
	

    public static class AtomicEndpointVariable
        extends AtomicValueVariable<Endpoint,Endpoint>
    {
        public AtomicEndpointVariable(boolean _log_changes,Object init_val)
        {
            super(
                _log_changes,(Endpoint)init_val,default_endpoint,
                endpoint_value_type_data_wrapper_factory);		
        }
        public AtomicEndpointVariable(boolean _log_changes)
        {
            super(
                _log_changes,default_endpoint,default_endpoint,
                endpoint_value_type_data_wrapper_factory);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Util.logger_assert(
                "Cannot pass reference to endpoint across network.");
        }
    }

    public static class AtomicServiceFactoryVariable
        extends AtomicValueVariable<InternalServiceFactory,InternalServiceFactory>
    {
        public AtomicServiceFactoryVariable(boolean _log_changes,Object init_val)
        {
            super(
                _log_changes,(InternalServiceFactory)init_val,default_service_factory,
                service_factory_value_type_data_wrapper_factory);		
        }
        public AtomicServiceFactoryVariable(boolean _log_changes)
        {
            super(
                _log_changes,default_service_factory,default_service_factory,
                service_factory_value_type_data_wrapper_factory);
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            serialize_service_factory(internal_val,any_builder,is_reference);
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
            boolean _dummy_log_changes, Double init_val)
        {
            super(
                init_val,default_number,
                number_value_type_data_wrapper_factory);
        }

        public NonAtomicNumberVariable(boolean _dummy_log_changes)
        {
            super(
                default_number,default_number,
                number_value_type_data_wrapper_factory);			
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setReference(is_reference);
        }
    }

    public static class NonAtomicTextVariable
        extends NonAtomicValueVariable<String,String>
    {
        public NonAtomicTextVariable(
            boolean _dummy_log_changes,String init_val)
        {
            super(
                init_val,default_text,
                text_value_type_data_wrapper_factory);
        }
		
        public NonAtomicTextVariable(boolean _dummy_log_changes)
        {
            super(
                default_text,default_text,
                text_value_type_data_wrapper_factory);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setReference(is_reference);
        }
        
    }

    public static class NonAtomicTrueFalseVariable
        extends NonAtomicValueVariable<Boolean,Boolean>
    {
        public NonAtomicTrueFalseVariable(
            boolean _dummy_log_changes, Boolean init_val)
        {
            super(
                init_val,default_tf,
                true_false_value_type_data_wrapper_factory);
        }

        public NonAtomicTrueFalseVariable(boolean _dummy_log_changes)
        {
            super(
                default_tf,default_tf,
                true_false_value_type_data_wrapper_factory);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setReference(is_reference);
        }
    }


    public static class NonAtomicEndpointVariable
        extends NonAtomicValueVariable<Endpoint,Endpoint>
    {
        public NonAtomicEndpointVariable(
            boolean _dummy_log_changes, Endpoint init_val)
        {
            super(
                init_val,default_endpoint,
                endpoint_value_type_data_wrapper_factory);
        }

        public NonAtomicEndpointVariable(boolean _dummy_log_changes)
        {
            super(
                default_endpoint,default_endpoint,
                endpoint_value_type_data_wrapper_factory);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            Util.logger_assert(
                "Cannot pass endpoint reference over network.");
        }
    }


    public static class NonAtomicServiceFactoryVariable
        extends NonAtomicValueVariable<InternalServiceFactory,InternalServiceFactory>
    {
        public NonAtomicServiceFactoryVariable(boolean _log_changes,InternalServiceFactory init_val)
        {
            super(
                init_val,default_service_factory,
                service_factory_value_type_data_wrapper_factory);		
        }
        public NonAtomicServiceFactoryVariable(boolean _dummy_log_changes)
        {
            super(
                default_service_factory,default_service_factory,
                service_factory_value_type_data_wrapper_factory);
        }

        @Override
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            InternalServiceFactory internal_val = get_val(active_event);
            AtomicServiceFactoryVariable.serialize_service_factory(
                internal_val,any_builder,is_reference);
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
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_index_type,locked_wrapper);
        }

        public NonAtomicMapVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalMap<K,V,D> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(internal_val,_index_type,locked_wrapper);
        }
    }

    // atomic
    public static class AtomicMapVariable <K,V,D>
        extends AtomicMap<K,V,D>
    {
        public AtomicMapVariable(
            boolean _log_changes,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_log_changes,_index_type,locked_wrapper);
        }

        public AtomicMapVariable(
            boolean _log_changes,
            AtomicInternalMap<K,V,D> internal_val,
            NonAtomicInternalMap.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(
                _log_changes,internal_val,_index_type,locked_wrapper);
        }
    }
    
    /************ Handling Lists ********/
    public static class AtomicListVariable <V,D>
        extends AtomicList<V,D>
    {
        public AtomicListVariable(
            boolean _log_changes,EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_log_changes,locked_wrapper);
        }


        public AtomicListVariable(
            boolean _log_changes,AtomicInternalList<V,D> internal_val,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_log_changes,internal_val,locked_wrapper);
        }
    }

    public static class NonAtomicListVariable <V,D>
        extends NonAtomicList<V,D>
    {
        public NonAtomicListVariable(
            boolean _dummy_log_changes, EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(locked_wrapper);
        }

        public NonAtomicListVariable(
            boolean _dummy_log_changes,
            NonAtomicInternalList<V,D> internal_val,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(internal_val,locked_wrapper);
        }
    }
}
