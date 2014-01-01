package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;

public class Variables {
    final static NumberTypeDataWrapperConstructor
        number_value_type_data_wrapper_constructor =
        new NumberTypeDataWrapperConstructor();
    final static Double default_number = new Double(0.0);

    final static TextTypeDataWrapperConstructor
        text_value_type_data_wrapper_constructor =
        new TextTypeDataWrapperConstructor();
    
    final static String default_text = new String();
	
    final static TrueFalseTypeDataWrapperConstructor
        true_false_value_type_data_wrapper_constructor = 
        new TrueFalseTypeDataWrapperConstructor();
    
    final static Boolean default_tf = false;
	
	
    public static class AtomicNumberVariable extends AtomicValueVariable<Double,Double>
    {
        public AtomicNumberVariable(String _host_uuid, boolean _peered,Object init_val)
        {
            super(
                _host_uuid,_peered, new Double(((Number) init_val).doubleValue()),
                default_number,number_value_type_data_wrapper_constructor);		
        }
        public AtomicNumberVariable(String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_number,default_number,
                number_value_type_data_wrapper_constructor);		
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

    public static class AtomicTextVariable extends AtomicValueVariable<String,String>
    {
        public AtomicTextVariable(String _host_uuid, boolean _peered,Object init_val)
        {
            super(
                _host_uuid,_peered,(String)init_val,default_text,
                text_value_type_data_wrapper_constructor);		
        }
        public AtomicTextVariable(String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_text,default_text,
                text_value_type_data_wrapper_constructor);            
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
	
    public static class AtomicTrueFalseVariable
        extends AtomicValueVariable<Boolean,Boolean>
    {
        public AtomicTrueFalseVariable(
            String _host_uuid, boolean _peered,Object init_val)
        {
            super(
                _host_uuid,_peered,(Boolean)init_val,default_tf,
                true_false_value_type_data_wrapper_constructor);		
        }
        public AtomicTrueFalseVariable(String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_tf,default_tf,
                true_false_value_type_data_wrapper_constructor);
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
	
	
    public static class NonAtomicNumberVariable
        extends NonAtomicValueVariable<Double,Double>
    {
        public NonAtomicNumberVariable(
            String _host_uuid, boolean _peered, Double init_val)
        {
            super(
                _host_uuid,_peered,init_val,default_number,
                number_value_type_data_wrapper_constructor);
        }

        public NonAtomicNumberVariable(
            String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_number,default_number,
                number_value_type_data_wrapper_constructor);			
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
            String _host_uuid, boolean _peered,String init_val)
        {
            super(
                _host_uuid,_peered,init_val,default_text,
                text_value_type_data_wrapper_constructor);
        }
		
        public NonAtomicTextVariable(
            String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_text,default_text,
                text_value_type_data_wrapper_constructor);
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
            String _host_uuid, boolean _peered, Boolean init_val)
        {
            super(
                _host_uuid,_peered,init_val,default_tf,
                true_false_value_type_data_wrapper_constructor);
        }

        public NonAtomicTrueFalseVariable(
            String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_tf,default_tf,
                true_false_value_type_data_wrapper_constructor);
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

    /************ Handling maps ********/
    public static class NonAtomicMapVariable <K,V,D>
        extends NonAtomicMap<K,V,D>
    {
        public NonAtomicMapVariable(
            String _host_uuid, boolean _peered,
            NonAtomicMapContainer.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_host_uuid,_peered,_index_type,locked_wrapper);
        }

        public NonAtomicMapVariable(
            String _host_uuid, boolean _peered,
            NonAtomicMapContainer<K,V,D> internal_val,
            NonAtomicMapContainer.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(
                _host_uuid, _peered,internal_val,_index_type,
                locked_wrapper);
        }
    }

    public static class AtomicMapVariable <K,V,D>
        extends AtomicMap<K,V,D>
    {
        public AtomicMapVariable(
            String _host_uuid, boolean _peered,
            NonAtomicMapContainer.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_host_uuid,_peered,_index_type,locked_wrapper);
        }


        public AtomicMapVariable(
            String _host_uuid, boolean _peered,
            AtomicMapContainer<K,V,D> internal_val,
            NonAtomicMapContainer.IndexType _index_type,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(
                _host_uuid, _peered,internal_val,_index_type,
                locked_wrapper);
        }
    }
    
    /************ Handling Lists ********/
    public static class AtomicListVariable <V,D>
        extends AtomicList<V,D>
    {
        public AtomicListVariable(
            String _host_uuid, boolean _peered,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(_host_uuid,_peered,locked_wrapper);
        }


        public AtomicListVariable(
            String _host_uuid, boolean _peered,
            AtomicListContainer<V,D> internal_val,
            EnsureAtomicWrapper<V,D> locked_wrapper)
        {
            super(
                _host_uuid, _peered,internal_val,
                locked_wrapper);
        }
    }
    
}
