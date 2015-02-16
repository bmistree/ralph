package ralph;

import java.util.Map.Entry;
import java.util.Set;

import RalphExceptions.BackoutException;

import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;
import RalphDataWrappers.MapTypeDataWrapperSupplier;
import RalphAtomicWrappers.BaseAtomicWrappers;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.ArgumentContainerDeltas;
import ralph_protobuffs.DeltaProto.Delta.ContainerDelta;
import ralph_protobuffs.DeltaProto.Delta;

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
    
    public RalphInternalMap(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
    }
    
    public void init_ralph_internal_map(
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper,
        MapTypeDataWrapperSupplier<K,V,ValueDeltaType>_data_wrapper_supplier,
        ImmediateCommitSupplier _immediate_commit_supplier)
    {
        locked_wrapper = _locked_wrapper;
        data_wrapper_supplier = _data_wrapper_supplier;
        immediate_commit_supplier = _immediate_commit_supplier;
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
       Caller must ensure that there will be no conflicts when
       writing.  Used for deserialization, not real operations.
     */
    @Override
    public void direct_set_val_on_key(K key, V to_write)
    {
        RalphObject<V,ValueDeltaType> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(to_write,ralph_globals);
        direct_set_val_on_key(key,wrapped_to_write);
    }
    /**
       Caller must ensure that there will be no conflicts when
       writing.  Used for deserialization, not real operations.
     */
    @Override
    public void direct_set_val_on_key(
        K key, RalphObject<V,ValueDeltaType> to_write)
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        // write directly to internal value.... caller must ensure no
        // conflicts when writing.
        wrapped_val.val.put(key,to_write);
    }
    @Override
    public void direct_remove_val_on_key(K key)
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        wrapped_val.val.remove(key);
    }
    @Override
    public void direct_clear()
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        wrapped_val.val.clear();
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


    public static <KeyType,ValueType,ValueDeltaType> ObjectContents serialize_contents(
        ActiveEvent active_event,String key_type_name, String value_type_name,
        SerializationContext serialization_context, boolean is_atomic,
        String internal_container_uuid,
        MapTypeDataWrapperSupplier<KeyType,ValueType,ValueDeltaType> data_wrapper_supplier,
        ImmediateCommitSupplier immediate_commit_supplier)
        throws BackoutException
    {
        if ((serialization_context != null) &&
            (serialization_context.deep_copy))
        {
            ArgumentContainerDeltas.Builder arg_container_deltas_builder = 
                ArgumentContainerDeltas.newBuilder();
            arg_container_deltas_builder.setObjectUuid(internal_container_uuid);

            // FIXME: check if internal map is null

            // FIXME: check if key is null
            
            MapTypeDataWrapper<KeyType,ValueType,ValueDeltaType> wrapped_map =
                data_wrapper_supplier.get_val_read(active_event);

            // run through all values in map and serialize them.
            for (Entry<KeyType,RalphObject<ValueType,ValueDeltaType>> entry :
                     wrapped_map.val.entrySet())
            {
                KeyType key = entry.getKey();
                
                // deeply-serializing this object by just creating a
                // message that adds all data in the map.  This means
                // that all op_types should be adds.
                ContainerDelta.Builder container_delta_builder =
                    ContainerDelta.newBuilder();
                container_delta_builder.setOpType(Delta.ContainerOpType.ADD);

                // serialize key type into map
                Delta.ValueType.Builder value_type_builder =
                    Delta.ValueType.newBuilder();
                if (key_type_name == Double.class.getName())
                {
                    if (key == null)
                        value_type_builder.setNullNum(true);
                    else
                    {
                        value_type_builder.setNum(
                            ((Double)key).doubleValue());
                    }
                }
                else if (key_type_name == String.class.getName())
                {
                    if (key == null)
                        value_type_builder.setNullText(true);
                    else
                        value_type_builder.setText((String)key);
                }
                else if (key_type_name == Boolean.class.getName())
                {
                    if (key == null)
                        value_type_builder.setNullTf(true);
                    else
                    {
                        value_type_builder.setTf(
                            ((Boolean)key).booleanValue());
                    }
                }
                //// DEBUG
                else
                {
                    Util.logger_assert(
                        "Unknown key type for map when serializing");
                }
                //// END DEBUG
                
                container_delta_builder.setKey(value_type_builder);

                // Serialize value types into maps.
                RalphObject<ValueType,ValueDeltaType> value =
                    entry.getValue();
                // tell the serialization_context to serialize the
                // internal ralph object contained withing this map.
                serialization_context.add_to_serialize(value);
                
                String value_reference = value.uuid();
                Delta.ReferenceType.Builder value_reference_builder =
                    Delta.ReferenceType.newBuilder();
                value_reference_builder.setReference(value_reference);

                container_delta_builder.setWhatAddedOrWritten(
                    value_reference_builder);

                // finally, add the container_delta just built to
                // ArgumentContainerDeltas.
                arg_container_deltas_builder.addContainerDelta(
                    container_delta_builder);
            }

            immediate_commit_supplier.check_immediate_commit(active_event);
            serialization_context.add_argument_container_delta(
                arg_container_deltas_builder);
        }


        // Build actual object contents for internal map
        ObjectContents.InternalMap.Builder internal_map_builder =
            ObjectContents.InternalMap.newBuilder();
        internal_map_builder.setKeyTypeClassName(key_type_name);
        internal_map_builder.setValTypeClassName(value_type_name);

        ObjectContents.Builder to_return = ObjectContents.newBuilder();
        to_return.setInternalMapType(internal_map_builder);
        to_return.setUuid(internal_container_uuid);
        to_return.setAtomic(is_atomic);
        return to_return.build();
    }
}
