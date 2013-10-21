package ralph;

import java.util.HashMap;
import java.util.Map.Entry;

import WaldoExceptions.BackoutException;

import waldo_protobuffs.VarStoreDeltasProto;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleNumberDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleTextDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleTrueFalseDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction;

public class LockedVariables {
    final static ValueTypeDataWrapperConstructor<Double,Double>
        number_value_type_data_wrapper_constructor =
        new ValueTypeDataWrapperConstructor<Double,Double>();
    final static Double default_number = new Double(0.0);

    final static ValueTypeDataWrapperConstructor<String,String>
        text_value_type_data_wrapper_constructor =
        new ValueTypeDataWrapperConstructor<String,String>();
    
    final static String default_text = new String();
	
    final static ValueTypeDataWrapperConstructor<Boolean,Boolean>
        true_false_value_type_data_wrapper_constructor = 
        new ValueTypeDataWrapperConstructor<Boolean,Boolean>();
    
    final static Boolean default_tf = false;
	
	
	
    public static LockedObject ensure_locked_obj(Object to_write,String host_uuid, boolean single_thread)
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
        /*
          else if (HashMap.class.isInstance(to_write))
          {
          Util.logger_warn(
          "Incorrectly assuming that always incorporating deltas in hash map");
          return new LockedVariables.SingleThreadedLockedMapVariable(host_uuid,false,(HashMap)to_write,true);
          }*/
        else if (LockedObject.class.isInstance(to_write))
        {
            return (LockedObject)to_write;
        }
		
        Util.logger_assert("Unknown type to ensure locked in SingleThreadedLockedMap");
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
		
        @Override
        protected boolean value_variable_py_val_serialize(
            waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.Builder parent_delta,
            Double var_data, String var_name) 
        {
            // can only add a pure number to var store a holder or to
            // an added key
            SingleNumberDelta.Builder delta = SingleNumberDelta.newBuilder();
            delta.setVarName(var_name);
            delta.setVarData(var_data.doubleValue());
            parent_delta.addNumDeltas(delta);			
            return true;
        }


        @Override
        protected boolean value_variable_py_val_serialize(
            ContainerAddedKey.Builder parent_delta,
            Double var_data, String var_name) 
        {
            //parent_delta.added_what_num = var_data
            parent_delta.setAddedWhatNum(var_data.doubleValue());
            return true;
        }

        @Override
        protected boolean value_variable_py_val_serialize(
            ContainerWriteKey.Builder parent_delta,
            Double var_data, String var_name) 
        {
            // parent.what_written_num = var_data
            parent_delta.setWhatWrittenNum(var_data.doubleValue());
            return true;
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

        @Override
        protected boolean value_variable_py_val_serialize(
            VarStoreDeltas.Builder parent_delta,
            String var_data, String var_name) 
        {
            // can only add a pure number to var store a holder or to
            // an added key
            SingleTextDelta.Builder delta = SingleTextDelta.newBuilder();
            delta.setVarName(var_name);
            delta.setVarData(var_data);
            parent_delta.addTextDeltas(delta);			
            return true;
        }

        @Override
        protected boolean value_variable_py_val_serialize(
            ContainerAddedKey.Builder parent_delta,
            String var_data, String var_name) 
        {
            parent_delta.setAddedWhatText(var_data);
            return true;
        }

        @Override
        protected boolean value_variable_py_val_serialize(
            ContainerWriteKey.Builder parent_delta,
            String var_data, String var_name) 
        {
            parent_delta.setWhatWrittenText(var_data);
            return true;
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

		
        @Override
        protected boolean value_variable_py_val_serialize(
            VarStoreDeltas.Builder parent_delta,
            Boolean var_data, String var_name) 
        {
            // can only add a pure number to var store a holder or to
            // an added key
            SingleTrueFalseDelta.Builder delta = SingleTrueFalseDelta.newBuilder();
            delta.setVarName(var_name);
            delta.setVarData(var_data);
            parent_delta.addTrueFalseDeltas(delta);			
            return true;
        }

        @Override
        protected boolean value_variable_py_val_serialize(
            ContainerAddedKey.Builder parent_delta,
            Boolean var_data, String var_name) 
        {
            parent_delta.setAddedWhatTf(var_data);
            return true;
        }

        @Override
        protected boolean value_variable_py_val_serialize(
            ContainerWriteKey.Builder parent_delta,
            Boolean var_data, String var_name) 
        {
            parent_delta.setWhatWrittenTf(var_data);
            return true;
        }
		
    }

	
    /**
     * 
     * @author bmistree
     *
     * @param <K>  ---- The keys used for indexing
     * @param <V>  ---- The type of each internal value in the internal hash map
     * @param <D>  ---- The type that each value in the internal hash map would dewaldoify into
     * 
     * An map of numbers to strings:
     * 
     * LockedMapVariable<Number,String,HashMap<String,Number>>
     * 
     * An map of numbers to maps of numbers to strings
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
		

        public SingleThreadedLockedMapVariable(
            String _host_uuid, boolean _peered,
            SingleThreadedLockedInternalMapVariable<K,V,D> init_val)
        {
            super(
                _host_uuid,_peered,
                // initial value
                init_val,
                // default value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid, _peered),
                new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());
        }
        public SingleThreadedLockedMapVariable(String _host_uuid)
        {
            super(
                _host_uuid,false,
                // initial value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid,false),
                // default value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid,false),
                new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());
        }		
        public SingleThreadedLockedMapVariable(String _host_uuid,boolean _peered)
        {
            super(
                _host_uuid,_peered, 
                // initial value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid,_peered),
                // default value
                new SingleThreadedLockedInternalMapVariable<K,V,D>(_host_uuid,_peered),
                new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());
        }
    }
}
