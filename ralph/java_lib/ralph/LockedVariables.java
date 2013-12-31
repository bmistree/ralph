package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;

public class LockedVariables {
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
	
	
    public static class LockedNumberVariable extends LockedValueVariable<Double,Double>
    {
        public LockedNumberVariable(String _host_uuid, boolean _peered,Object init_val)
        {
            super(
                _host_uuid,_peered, new Double(((Number) init_val).doubleValue()),
                default_number,number_value_type_data_wrapper_constructor);		
        }
        public LockedNumberVariable(String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_number,default_number,
                number_value_type_data_wrapper_constructor);		
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setReference(is_reference);
        }
    }

    public static class LockedTextVariable extends LockedValueVariable<String,String>
    {
        public LockedTextVariable(String _host_uuid, boolean _peered,Object init_val)
        {
            super(
                _host_uuid,_peered,(String)init_val,default_text,
                text_value_type_data_wrapper_constructor);		
        }
        public LockedTextVariable(String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_text,default_text,
                text_value_type_data_wrapper_constructor);            
        }
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setReference(is_reference);
        }
    }
	
    public static class LockedTrueFalseVariable
        extends LockedValueVariable<Boolean,Boolean>
    {
        public LockedTrueFalseVariable(
            String _host_uuid, boolean _peered,Object init_val)
        {
            super(
                _host_uuid,_peered,(Boolean)init_val,default_tf,
                true_false_value_type_data_wrapper_constructor);		
        }
        public LockedTrueFalseVariable(String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_tf,default_tf,
                true_false_value_type_data_wrapper_constructor);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            Boolean internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setTrueFalse(internal_val.booleanValue());
            any_builder.setReference(is_reference);
        }
    }
	
	
    public static class SingleThreadedLockedNumberVariable
        extends SingleThreadedLockedValueVariable<Double,Double>
    {
        public SingleThreadedLockedNumberVariable(
            String _host_uuid, boolean _peered, Double init_val)
        {
            super(
                _host_uuid,_peered,init_val,default_number,
                number_value_type_data_wrapper_constructor);
        }

        public SingleThreadedLockedNumberVariable(
            String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_number,default_number,
                number_value_type_data_wrapper_constructor);			
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
            boolean is_reference)
        {
            Double internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setNum(internal_val.doubleValue());
            any_builder.setReference(is_reference);
        }
    }

    public static class SingleThreadedLockedTextVariable
        extends SingleThreadedLockedValueVariable<String,String>
    {
        public SingleThreadedLockedTextVariable(
            String _host_uuid, boolean _peered,String init_val)
        {
            super(
                _host_uuid,_peered,init_val,default_text,
                text_value_type_data_wrapper_constructor);
        }
		
        public SingleThreadedLockedTextVariable(
            String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_text,default_text,
                text_value_type_data_wrapper_constructor);
        }
        
        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            String internal_val = get_val(active_event);
            any_builder.setVarName("");
            any_builder.setText(internal_val);
            any_builder.setReference(is_reference);
        }
        
    }

    public static class SingleThreadedLockedTrueFalseVariable
        extends SingleThreadedLockedValueVariable<Boolean,Boolean>
    {
        public SingleThreadedLockedTrueFalseVariable(
            String _host_uuid, boolean _peered, Boolean init_val)
        {
            super(
                _host_uuid,_peered,init_val,default_tf,
                true_false_value_type_data_wrapper_constructor);
        }

        public SingleThreadedLockedTrueFalseVariable(
            String _host_uuid, boolean _peered)
        {
            super(
                _host_uuid,_peered,default_tf,default_tf,
                true_false_value_type_data_wrapper_constructor);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
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
            EnsureLockedWrapper<V,D> locked_wrapper)
        {
            super(_host_uuid,_peered,_index_type,locked_wrapper);
        }

        public NonAtomicMapVariable(
            String _host_uuid, boolean _peered,
            NonAtomicMapContainer<K,V,D> internal_val,
            NonAtomicMapContainer.IndexType _index_type,
            EnsureLockedWrapper<V,D> locked_wrapper)
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
            EnsureLockedWrapper<V,D> locked_wrapper)
        {
            super(_host_uuid,_peered,_index_type,locked_wrapper);
        }


        public AtomicMapVariable(
            String _host_uuid, boolean _peered,
            AtomicMapContainer<K,V,D> internal_val,
            NonAtomicMapContainer.IndexType _index_type,
            EnsureLockedWrapper<V,D> locked_wrapper)
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
            EnsureLockedWrapper<V,D> locked_wrapper)
        {
            super(_host_uuid,_peered,locked_wrapper);
        }


        public AtomicListVariable(
            String _host_uuid, boolean _peered,
            AtomicListContainer<V,D> internal_val,
            EnsureLockedWrapper<V,D> locked_wrapper)
        {
            super(
                _host_uuid, _peered,internal_val,
                locked_wrapper);
        }
    }
    
}
