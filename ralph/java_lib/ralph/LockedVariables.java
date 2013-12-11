package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;
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
	
	
	
    public static LockedObject ensure_locked_obj(
        Object to_write,String host_uuid, boolean single_thread)
    {
        if (Boolean.class.isInstance(to_write))
        {
            return
                new LockedVariables.SingleThreadedLockedTrueFalseVariable(
                    host_uuid,false,(Boolean) to_write);
        }
        else if (Double.class.isInstance(to_write))
        {
            return
                new LockedVariables.SingleThreadedLockedNumberVariable(
                    host_uuid,false,(Double) to_write);
        }
        else if (String.class.isInstance(to_write))
        {
            return
                new LockedVariables.SingleThreadedLockedTextVariable(
                    host_uuid,false,(String) to_write);
        }
        else if (LockedObject.class.isInstance(to_write))
        {
            return (LockedObject)to_write;
        }
		
        Util.logger_assert(
            "Unknown type to ensure locked in SingleThreadedLockedMap");
        return null;
    }
	
	
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

    /**
     * @param <K>  ---- The keys used for indexing
     * @param <V>  ---- The type of each internal value in the internal hash map
     * @param <D>  ---- The type that each value in the internal hash map would dewaldoify into
     * 
     * A map of numbers to strings:
     * 
     * LockedMapVariable<Number,String,HashMap<String,Number>>
     * 
     * A map of numbers to maps of numbers to strings
     * 
     * LockedMapVariable<
     *     Number,
     *     LockedMapVariable< Number, String, HashMap<String,Number > >
     *     HashMap<Number,HashMap<String,Number>>>
     * 
     */
    public static class SingleThreadedLockedMapVariable<K,V,D>
        extends SingleThreadedContainerReference<K,V,D>
    {
        public SingleThreadedLockedMapVariable(
            String _host_uuid, boolean _peered,
            HashMap<K,LockedObject<V,D>> init_val,boolean incorporating_deltas)
        {
            // FIXME: I'm pretty sure that the type signature for the locked object above
            // is incorrect: it shouldn't be D, right?			
            super(
                _host_uuid,_peered,
                // initial value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid,false),
                // default value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid, _peered),
                new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());

            load_init_vals(init_val,incorporating_deltas);
        }

        public void serialize_as_rpc_arg(
            ActiveEvent active_event,Variables.Any.Builder any_builder,
            boolean is_reference) throws BackoutException
        {
            SingleThreadedLockedContainer<K,V,D> internal_val =
                get_val(active_event);
            internal_val.serialize_as_rpc_arg(
                active_event,any_builder,is_reference);
        }
        
        public SingleThreadedLockedMapVariable(
            String _host_uuid, boolean _peered,
            HashMap<K,LockedObject<V,D>> init_val)
        {
            // FIXME: I'm pretty sure that the type signature for the locked object above
            // is incorrect: it shouldn't be D, right?			
            super(
                _host_uuid,_peered,
                // initial value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid,false),
                // default value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid, _peered),
                new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());

            load_init_vals(init_val,false);
        }
    

        public void load_init_vals(
            HashMap<K,LockedObject<V,D>> init_val, boolean incorporating_deltas)
        {
            if (init_val == null)
                return;
        
            //FIXME probably inefficient to add each field separately
            for (Entry<K, LockedObject<V,D>> entry : init_val.entrySet())
            {
                ReferenceTypeDataWrapper<K,V,D>casted_wrapper = (ReferenceTypeDataWrapper<K,V,D>)val.val.val;
				
                // single threaded variables will not throw backout exceptions.
                try {
                    casted_wrapper.set_val_on_key(
                        null, entry.getKey(), entry.getValue(), incorporating_deltas);
                } catch (BackoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
