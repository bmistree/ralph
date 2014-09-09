package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;
import RalphDataWrappers.MapTypeDataWrapperSupplier;
import RalphAtomicWrappers.BaseAtomicWrappers;

/**
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that the keys should point to.
 */
public class RalphInternalMap<K,V,ValueDeltaType> 
    implements RalphInternalMapInterface<K,V,ValueDeltaType>
{
    private EnsureAtomicWrapper<V,ValueDeltaType>locked_wrapper;
    private MapTypeDataWrapperSupplier<K,V,ValueDeltaType> data_wrapper_supplier;
    private ImmediateCommitSupplier immediate_commit_supplier;
    private RalphGlobals ralph_globals = null;
    
    // Keeps track of the map's index type.  Useful when serializing
    // and deserializing data.
    public NonAtomicInternalMap.IndexType index_type;
    
    public RalphInternalMap(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
    }
    
    public void init_ralph_internal_map(
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper,
        MapTypeDataWrapperSupplier<K,V,ValueDeltaType>_data_wrapper_supplier,
        ImmediateCommitSupplier _immediate_commit_supplier,
        NonAtomicInternalMap.IndexType _index_type)
    {
        locked_wrapper = _locked_wrapper;
        data_wrapper_supplier = _data_wrapper_supplier;
        immediate_commit_supplier = _immediate_commit_supplier;
        index_type = _index_type;
    }

    private MapTypeDataWrapper<K,V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        return data_wrapper_supplier.get_val_read(active_event);
    }
    private MapTypeDataWrapper<K,V,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        return data_wrapper_supplier.get_val_write(active_event);
    }
    private void check_immediate_commit(ActiveEvent active_event)
        throws BackoutException
    {
        immediate_commit_supplier.check_immediate_commit(active_event);
    }

    @Override
    public V get_val_on_key(ActiveEvent active_event, K key) throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        RalphObject<V,ValueDeltaType> internal_key_val = wrapped_val.val.get(key);

        Object to_return = null;        
        if (internal_key_val.return_internal_val_from_container())
            to_return = internal_key_val.get_val(active_event);
        else
            to_return = internal_key_val;
        
        check_immediate_commit(active_event);
        return (V)to_return;
    }

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    @Override
    public void serialize_as_rpc_arg (
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_write(active_event);

        VariablesProto.Variables.Map.Builder map_builder =
            VariablesProto.Variables.Map.newBuilder();

        // set identifier types
        if (index_type == NonAtomicInternalMap.IndexType.DOUBLE)
        {
            map_builder.setKeyTypeIdentifier(
                BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL);
        }
        else if (index_type == NonAtomicInternalMap.IndexType.STRING)
        {
            map_builder.setKeyTypeIdentifier(
                BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL);
        }
        else if (index_type == NonAtomicInternalMap.IndexType.BOOLEAN)
        {
            map_builder.setKeyTypeIdentifier(
                BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL);
        }
        //// DEBUG
        else
            Util.logger_assert("Unkonw map index type in serialize");
        //// END DEBUG

        
        map_builder.setValueTypeIdentifier(
            locked_wrapper.get_serialization_label());

        // serialize internal values        
        for (Entry<K,RalphObject<V,ValueDeltaType>> map_entry : wrapped_val.val.entrySet() )
        {
            // create any for index
            VariablesProto.Variables.Any.Builder index_builder = VariablesProto.Variables.Any.newBuilder();
            index_builder.setVarName("");
            if (index_type == NonAtomicInternalMap.IndexType.DOUBLE)
            {
                Double index_entry = (Double) map_entry.getKey();
                index_builder.setNum(index_entry.doubleValue());
                index_builder.setIsTvar(false);                
            }
            else if (index_type == NonAtomicInternalMap.IndexType.STRING)
            {
                String index_entry = (String) map_entry.getKey();
                index_builder.setText(index_entry);
                index_builder.setIsTvar(false);
            }
            else if (index_type == NonAtomicInternalMap.IndexType.BOOLEAN)
            {
                Boolean index_entry = (Boolean) map_entry.getKey();
                index_builder.setTrueFalse(index_entry.booleanValue());
                index_builder.setIsTvar(false);
            }
            else
            {
                Util.logger_assert(
                    "Unrecognized index type when serializing matrix");
            }

            // create any for value
            VariablesProto.Variables.Any.Builder value_builder =
                VariablesProto.Variables.Any.newBuilder();
            RalphObject<V,ValueDeltaType> map_value = map_entry.getValue();
            
            map_value.serialize_as_rpc_arg(active_event,value_builder);
            
            // apply both to map builder
            map_builder.addMapIndices(index_builder);
            map_builder.addMapValues(value_builder);
        }
        any_builder.setVarName("");
        any_builder.setMap(map_builder);

        check_immediate_commit(active_event);
    }
    

    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException
    {
        RalphObject<V,ValueDeltaType> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(to_write,ralph_globals);
        set_val_on_key(active_event,key,wrapped_to_write);
    }

    @Override	
    public void set_val_on_key(
        ActiveEvent active_event, K key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException 
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_write(active_event);
        wrapped_val.set_val_on_key(active_event,key,to_write);
        check_immediate_commit(active_event);
    }


    @Override
    public boolean return_internal_val_from_container()
    {
        Util.logger_warn(
            "Warning: check in multithreaded container whether " +
            "should return internal value");
        return false;
    }


    @Override
    public int get_len(ActiveEvent active_event) throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        int size = wrapped_val.val.size();
        check_immediate_commit(active_event);
        return size;
    }

    @Override
    public Double get_len_boxed(ActiveEvent active_event) 
        throws BackoutException
    {
        return new Double(get_len(active_event));
    }

    @Override
    public Set<K> get_iterable(ActiveEvent active_event)
        throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        Set<K> to_return = wrapped_val.val.keySet();
        check_immediate_commit(active_event);
        return to_return;
    }
    

    @Override
    public void remove(ActiveEvent active_event, K key_to_delete)
        throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_write(active_event);
        wrapped_val.del_key(active_event, key_to_delete);
        check_immediate_commit(active_event);
    }

    @Override
    public boolean contains_key_called(
        ActiveEvent active_event, K contains_key)  throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        boolean to_return = wrapped_val.val.containsKey(contains_key);
        check_immediate_commit(active_event);
        return to_return;
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
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        boolean to_return = wrapped_val.val.containsValue(contains_val);
        check_immediate_commit(active_event);
        return to_return;
    }

    @Override
    public void clear(ActiveEvent active_event) throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val = get_val_write(active_event);
        wrapped_val.val.clear();
        check_immediate_commit(active_event);
    }    
}
