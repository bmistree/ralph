package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;

/**
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that the keys should point to.
 * @param <D> --- The Java type of data that the internal locked
 * objects should dewaldoify into
 */
public class NonAtomicMapContainer<K,V,D> 
    extends NonAtomicObject <
    // The internal values that these are holding
    HashMap<K,RalphObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    HashMap<K,D>
    >  
    implements ContainerInterface<K,V,D>
{
    public enum IndexType{
        DOUBLE,STRING,BOOLEAN
    };

    public EnsureAtomicWrapper<V,D>locked_wrapper;
    
    // Keeps track of the map's index type.  Useful when serializing
    // and deserializing data.
    public IndexType index_type;
    
    private MapTypeDataWrapperFactory <K,V,D> reference_data_wrapper_constructor =
        null;

    private MapTypeDataWrapper<K,V,D> reference_type_val = null;
	
    public NonAtomicMapContainer()
    {
        super();
    }
    public void init(
        String _host_uuid, boolean _peered,
        MapTypeDataWrapperFactory<K,V,D> rtdwc,
        HashMap<K,RalphObject<V,D>>init_val,
        IndexType _index_type,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        index_type = _index_type;
        
        host_uuid = _host_uuid;
        peered = _peered;
        locked_wrapper = _locked_wrapper;
        reference_data_wrapper_constructor = rtdwc;
        reference_type_val =
            (MapTypeDataWrapper<K, V, D>)
            reference_data_wrapper_constructor.construct(init_val, peered); 
        val = reference_type_val;		
    }

    @Override
    public V get_val_on_key(ActiveEvent active_event, K key) throws BackoutException
    {
        /*
         *
         internal_key_val = self.val.val[key]
         if internal_key_val.return_internal_val_from_container():
         return internal_key_val.get_val(active_event)
         return internal_key_val
        */
        RalphObject<V,D> internal_key_val = val.val.get(key);
		
        if (internal_key_val.return_internal_val_from_container())
        {
            Object to_return = null;
            try {
                to_return = internal_key_val.get_val(active_event);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return (V)to_return;
        }
		
        return (V) internal_key_val;
    }

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        VariablesProto.Variables.Map.Builder map_builder = VariablesProto.Variables.Map.newBuilder();
        for (Entry<K,RalphObject<V,D>> map_entry : val.val.entrySet() )
        {
            // create any for index
            VariablesProto.Variables.Any.Builder index_builder = VariablesProto.Variables.Any.newBuilder();
            index_builder.setVarName("");
            if (index_type == IndexType.DOUBLE)
            {
                Double index_entry = (Double) map_entry.getKey();
                index_builder.setNum(index_entry.doubleValue());
            }
            else if (index_type == IndexType.STRING)
            {
                String index_entry = (String) map_entry.getKey();
                index_builder.setText(index_entry);
            }
            else if (index_type == IndexType.BOOLEAN)
            {
                Boolean index_entry = (Boolean) map_entry.getKey();
                index_builder.setTrueFalse(index_entry.booleanValue());
            }
            else
            {
                Util.logger_assert(
                    "Unrecognized index type when serializing matrix");
            }

            
            // create any for value
            VariablesProto.Variables.Any.Builder value_builder = VariablesProto.Variables.Any.newBuilder();
            RalphObject<V,D> map_value = map_entry.getValue();
            
            map_value.serialize_as_rpc_arg(
                active_event,value_builder,is_reference);
            
            // apply both to map builder
            map_builder.addMapIndices(index_builder);
            map_builder.addMapValues(value_builder);
        }
        any_builder.setVarName("");
        any_builder.setMap(map_builder);
        any_builder.setReference(is_reference);            
    }
    

    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);		
    }
	
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        // note: may need to change this to cast to RalphObject<V,D> and use other set_val.
        Util.logger_assert(
            "Should never be setting value directly on container.  " +
            "Instead, should have wrapped V in a RalphObject at an earlier call.");
		
    }	
    public void set_val_on_key(
        ActiveEvent active_event, K key, RalphObject<V,D> to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }


	
    public void set_val_on_key(
        ActiveEvent active_event, K key, RalphObject<V,D> to_write,
        boolean copy_if_peered) throws BackoutException 
    {
        //def set_val_on_key(self,active_event,key,to_write,copy_if_peered=False):
        //    if copy_if_peered:
        //        if isinstance(to_write,WaldoLockedObj):
        //            to_write = to_write.copy(active_event,True,True)
        //    return self.val.set_val_on_key(active_event,key,to_write)
		
        if (copy_if_peered)
        {
            try {
                to_write = to_write.copy(active_event, true, true);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        reference_type_val.set_val_on_key(active_event,key,to_write);
    }

    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        Util.logger_assert(
            "Still must define swap method for NonAtomicMapContainer.");
    }

    
    @Override
    public HashMap<K, RalphObject<V,D>> get_val(ActiveEvent active_event)
    {
    	Util.logger_assert("Cannot call get val on a container object.");
    	return null;
    }
    
    @Override
    public void set_val(
        ActiveEvent active_event,
        HashMap<K,RalphObject<V,D>> val_to_set_to)
    {
    	Util.logger_assert("Cannot call set val on a container object directly.");
    }

    @Override
    public void write_if_different(
        ActiveEvent active_event,
        HashMap<K, RalphObject<V,D>> new_val)
    {
        // should only call this method on a value type
        Util.logger_assert("Unable to call write if different on container");
    }

    @Override
    public int get_len(ActiveEvent active_event) throws BackoutException
    {
        return val.val.size();
    }

    @Override
    public Double get_len_boxed(ActiveEvent active_event) 
        throws BackoutException
    {
        return new Double(get_len(active_event));
    }

    @Override
    public ArrayList<K> get_keys(ActiveEvent active_event)
        throws BackoutException
    {
        return new ArrayList<K>(val.val.keySet());
    }

    @Override
    public void del_key_called(ActiveEvent active_event, K key_to_delete) throws BackoutException
    {
        reference_type_val.del_key(active_event, key_to_delete);
    }

    @Override
    public boolean contains_key_called(
        ActiveEvent active_event,
        K contains_key) throws BackoutException
    {
        return val.val.containsKey(contains_key);
    }

    @Override
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, K contains_key) throws BackoutException
    {
        return new Boolean(
            contains_key_called(active_event,contains_key));
    }    
    
    @Override
    public boolean contains_val_called(
        ActiveEvent active_event,
        V contains_val) throws BackoutException
    {
        return val.val.containsValue(contains_val);
    }

    @Override
    public void insert(
        ActiveEvent active_event, K index_to_insert_in, V what_to_insert)
        throws BackoutException
    {
        Util.logger_assert(
            "insert method only defined for lists, not maps.");
    }
    @Override
    public void insert(
        ActiveEvent active_event, K index_to_insert_in,
        RalphObject<V,D> what_to_insert)
        throws BackoutException
    {
        Util.logger_assert(
            "insert method only defined for lists, not maps.");
    }
}
