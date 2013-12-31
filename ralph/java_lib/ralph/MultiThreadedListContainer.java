package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;


/**
 * @param <V> --- The Java type of data that are elements in the list
 * @param <D> --- The Java type of data that elements should dewaldoify into.
 */
public class MultiThreadedListContainer<V,D> 
    extends MultiThreadedLockedObject <
    // The internal values that these are holding
    ArrayList<LockedObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    ArrayList<D>
    >  
    implements ContainerInterface<Integer,V,D>
{
    public EnsureLockedWrapper<V,D>locked_wrapper;
    
    public MultiThreadedListContainer()
    {
        super();
    }
    
    public void init_multithreaded_list_container(
        String _host_uuid, boolean _peered,
        ListTypeDataWrapperConstructor<V,D> rtdwc,
        ArrayList<LockedObject<V,D>>init_val,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        locked_wrapper = _locked_wrapper;
        init_multithreaded_locked_object(
            rtdwc,_host_uuid, _peered, init_val);
    }

    @Override
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in,
        V what_to_insert)
        throws BackoutException
    {
        Util.logger_assert(
            "Need to override list inertion code.");
    }
    
    @Override
    public void insert(
        ActiveEvent active_event, Integer key,
        LockedObject<V,D> to_insert)  throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_write_lock(active_event);

        wrapped_val.insert(active_event,key,to_insert);
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
    public V get_val_on_key(
        ActiveEvent active_event, Integer key) throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_read_lock(active_event);
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


    public void append(
        ActiveEvent active_event, V what_to_insert)
        throws BackoutException
    {
        LockedObject<V,D> wrapped_to_insert =
            locked_wrapper.ensure_locked_object(what_to_insert);

        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_write_lock(active_event);
        int size = wrapped_val.val.size();
        Integer index_to_insert_in = new Integer(size);
        insert(
            active_event, index_to_insert_in, wrapped_to_insert);
    }

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        Util.logger_assert(
            "Need to add list serialization code.");
    }
    

    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);		
    }
	
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        // note: may need to change this to cast to LockedObject<V,D> and use other set_val.
        Util.logger_assert(
            "Should never be setting value directly on container.  " +
            "Instead, should have wrapped V in a LockedObject at an earlier call.");		
    }	
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, LockedObject<V,D> to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }

	
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, LockedObject<V,D> to_write,
        boolean copy_if_peered) throws BackoutException 
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_write_lock(active_event);

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
    public ArrayList<LockedObject<V,D>> get_val(ActiveEvent active_event)
    {
    	Util.logger_assert("Cannot call get val on a container object.");
    	return null;
    }
    
    @Override
    public void set_val(
        ActiveEvent active_event,
        ArrayList<LockedObject<V,D>> val_to_set_to)
    {
    	Util.logger_assert("Cannot call set val on a container object directly.");
    }

    @Override
    public void write_if_different(
        ActiveEvent active_event,
        ArrayList<LockedObject<V,D>> new_val)
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
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_read_lock(active_event);
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
    public ArrayList<Integer> get_keys(ActiveEvent active_event)
        throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_read_lock(active_event);

        Util.logger_assert(
            "FIXME: should be returning LockedObject of lists.");
        
        // ArrayList<LockedObject<Integer> to_return = new ArrayList<K>(wrapped_val.val.keySet());
        // if (active_event.immediate_complete())
        // {
        //     // non-atomics should immediately commit their changes.  Note:
        //     // it's fine to presuppose this commit without backout because
        //     // we've defined non-atomic events to never backout of their
        //     // currrent commits.
        //     complete_commit(active_event);
        // }
        // return to_return;
        return null;
    }

    @Override
    public void del_key_called(ActiveEvent active_event, Integer key_to_delete)
        throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_write_lock(active_event);
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
        Integer contains_key)  throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_read_lock(active_event);

        int internal_val = contains_key.intValue();
        int list_size = wrapped_val.val.size();

        boolean to_return = internal_val < list_size;
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
        ActiveEvent active_event, Integer contains_key) throws BackoutException
    {
        return new Boolean(
            contains_key_called(active_event,contains_key));
    }    
    
    @Override
    public boolean contains_val_called(
        ActiveEvent active_event,
        V contains_val) throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_read_lock(active_event);

        Util.logger_assert(
            "FIXME: must fill in contains_val_called for lists.");
        return false;
        // boolean to_return = wrapped_val.val.containsValue(contains_val);
        // if (active_event.immediate_complete())
        // {
        //     // non-atomics should immediately commit their changes.  Note:
        //     // it's fine to presuppose this commit without backout because
        //     // we've defined non-atomic events to never backout of their
        //     // currrent commits.
        //     complete_commit(active_event);
        // }
        // return to_return;
    }
}
