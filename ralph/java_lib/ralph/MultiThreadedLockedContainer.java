package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import ralph.SingleThreadedLockedContainer.IndexType;

/**
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that the keys should point to.
 * @param <D> --- The Java type of data that the internal locked
 * objects should dewaldoify into
 */
public class MultiThreadedLockedContainer<K,V,D> 
    extends MultiThreadedLockedObject <
    // The internal values that these are holding
    HashMap<K,LockedObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    HashMap<K,D>
    >  
    implements ContainerInterface<K,V,D>
{
    // Keeps track of the map's index type.  Useful when serializing
    // and deserializing data.
    public IndexType index_type;
    
    public EnsureLockedWrapper<V,D>locked_wrapper;
    
    public MultiThreadedLockedContainer()
    {
        super();
    }

    public void init_multithreaded_locked_container(
        String _host_uuid, boolean _peered,
        ReferenceTypeDataWrapperConstructor<K,V,D> rtdwc,
        HashMap<K,LockedObject<V,D>>init_val,
        IndexType _index_type,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        index_type = _index_type;
        locked_wrapper = _locked_wrapper;
        init_multithreaded_locked_object(
            rtdwc,_host_uuid, _peered, init_val);
    }

    @Override
    public V get_val_on_key(ActiveEvent active_event, K key) throws BackoutException
    {
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);
        LockedObject<V,D> internal_key_val = wrapped_val.val.get(key);

        Object to_return = null;        
        if (internal_key_val.return_internal_val_from_container())
        {
            try {
                to_return = internal_key_val.get_val(active_event);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                to_return = internal_key_val;
                e.printStackTrace();
            }
        }
        else
            to_return = internal_key_val;

        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
        return (V)to_return;
    }

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);

        Variables.Map.Builder map_builder = Variables.Map.newBuilder();
        for (Entry<K,LockedObject<V,D>> map_entry : wrapped_val.val.entrySet() )
        {
            // create any for index
            Variables.Any.Builder index_builder = Variables.Any.newBuilder();
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
            Variables.Any.Builder value_builder = Variables.Any.newBuilder();
            LockedObject<V,D> map_value = map_entry.getValue();
            
            map_value.serialize_as_rpc_arg(
                active_event,value_builder,is_reference);
            
            // apply both to map builder
            map_builder.addMapIndices(index_builder);
            map_builder.addMapValues(value_builder);
        }
        any_builder.setVarName("");
        any_builder.setMap(map_builder);
        any_builder.setReference(is_reference);
        
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
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
        // note: may need to change this to cast to LockedObject<V,D> and use other set_val.
        Util.logger_assert(
            "Should never be setting value directly on container.  " +
            "Instead, should have wrapped V in a LockedObject at an earlier call.");		
    }	
    public void set_val_on_key(
        ActiveEvent active_event, K key, LockedObject<V,D> to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }

	
    public void set_val_on_key(
        ActiveEvent active_event, K key, LockedObject<V,D> to_write,
        boolean copy_if_peered) throws BackoutException 
    {
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_write_lock(active_event);

        if (copy_if_peered)
        {
            try {
                to_write = to_write.copy(active_event, true, true);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        wrapped_val.set_val_on_key(active_event,key,to_write);
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
    }

	
    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,LockedObject to_swap_with)
        throws BackoutException
    {
        Util.logger_assert(
            "Still must define swap method for MultiThreadedLockedContainer.");
    }

    
    @Override
    public HashMap<K, LockedObject<V,D>> get_val(ActiveEvent active_event)
    {
    	Util.logger_assert("Cannot call get val on a container object.");
    	return null;
    }
    
    @Override
    public void set_val(
        ActiveEvent active_event,
        HashMap<K,LockedObject<V,D>> val_to_set_to)
    {
    	Util.logger_assert("Cannot call set val on a container object directly.");
    }

    @Override
    public void write_if_different(
        ActiveEvent active_event,
        HashMap<K, LockedObject<V,D>> new_val)
    {
        // should only call this method on a value type
        Util.logger_assert("Unable to call write if different on container");
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
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);
        int size = wrapped_val.val.size();
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }

        return size;
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
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);

        
        ArrayList<K> to_return = new ArrayList<K>(wrapped_val.val.keySet());
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
        return to_return;
    }

    @Override
    public void del_key_called(ActiveEvent active_event, K key_to_delete)
        throws BackoutException
    {
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_write_lock(active_event);
        wrapped_val.del_key(active_event, key_to_delete);
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
    }

    @Override
    public boolean contains_key_called(
        ActiveEvent active_event,
        K contains_key)  throws BackoutException
    {
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);
        boolean to_return = wrapped_val.val.containsKey(contains_key);
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
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
        ReferenceTypeDataWrapper<K,V,D> wrapped_val =
            (ReferenceTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);
        boolean to_return = wrapped_val.val.containsValue(contains_val);
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
        return to_return;
    }
}
