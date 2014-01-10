package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperSupplier;

/**
 * @param <V> --- The Java type of data that are elements in the list
 * @param <D> --- The Java type of data that elements should
 * dewaldoify into.
 */
public class AtomicInternalList<V,D> 
    extends AtomicObject <
    // The internal values that these are holding
    ArrayList<RalphObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    ArrayList<D>
    >
    implements ImmediateCommitSupplier, ListTypeDataWrapperSupplier,
               RalphInternalListInterface<V,D>
{
    private RalphInternalList<V,D> internal_list = null;
    public EnsureAtomicWrapper<V,D> locked_wrapper = null;
    
    public AtomicInternalList()
    {
        super();
        internal_list = new RalphInternalList<V,D>();
    }
        
    public void init_multithreaded_list_container(
        String _host_uuid, boolean _log_changes,
        ListTypeDataWrapperFactory<V,D> ltdwf,
        ArrayList<RalphObject<V,D>>init_val,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        locked_wrapper = _locked_wrapper;
        init_multithreaded_locked_object(
            ltdwf,_host_uuid, _log_changes, init_val);
        internal_list.init_ralph_internal_list(
            _locked_wrapper,this,this);
    }

    @Override
    public void write_if_different(
        ActiveEvent active_event, ArrayList<RalphObject<V,D>> new_val)
        throws BackoutException
    {
        Util.logger_assert(
            "Must fill in write_if_different in NonAtomicInternalList.");
    }
    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        Util.logger_assert(
            "Must fill in write_if_different in NonAtomicInternalList.");
    }
    
    /** ImmediateCommitSupplier interface*/
    @Override
    public void check_immediate_commit(ActiveEvent active_event)
        throws BackoutException
    {
        if (active_event.immediate_complete())
        {
            // non-atomics should immediately commit their changes.  Note:
            // it's fine to presuppose this commit without backout because
            // we've defined non-atomic events to never backout of their
            // currrent commits.
            complete_commit(active_event);
        }
    }

    
    /** ListTypeDataWrapperSupplier Interface */
    @Override    
    public ListTypeDataWrapper<V,D> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_read_lock(active_event);
        return wrapped_val;
    }
    @Override    
    public ListTypeDataWrapper<V,D> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val =
            (ListTypeDataWrapper<V,D>)acquire_write_lock(active_event);
        return wrapped_val;
    }

    

    /** RalphInternalListInterface<V,D> Interface */
    @Override    
    public void insert(
        ActiveEvent active_event, Double index_to_insert_in,
        V what_to_insert) throws BackoutException
    {
        internal_list.insert(active_event,index_to_insert_in,what_to_insert);
    }
    @Override    
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in,
        V what_to_insert) throws BackoutException
    {
        internal_list.insert(active_event,index_to_insert_in,what_to_insert);
    }
    @Override    
    public void insert(
        ActiveEvent active_event, Integer key,
        RalphObject<V,D> to_insert)  throws BackoutException
    {
        internal_list.insert(active_event,key,to_insert);
    }

    @Override
    public V get_val_on_key(
        ActiveEvent active_event, Double key) throws BackoutException
    {
        return internal_list.get_val_on_key(active_event,key);
    }
    @Override
    public V get_val_on_key(
        ActiveEvent active_event, Integer key) throws BackoutException
    {
        return internal_list.get_val_on_key(active_event,key);
    }
    
    @Override
    public void append(
        ActiveEvent active_event, V what_to_insert) throws BackoutException
    {
        internal_list.append(active_event,what_to_insert);
    }
    
    @Override
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        internal_list.serialize_as_rpc_arg(active_event,any_builder,is_reference);
    }

    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write) throws BackoutException
    {
        internal_list.set_val_on_key(active_event,key,to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key, V to_write) throws BackoutException
    {
        internal_list.set_val_on_key(active_event,key,to_write);
    }
    
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,D> to_write)
        throws BackoutException
    {
        internal_list.set_val_on_key(active_event,key,to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,D> to_write)
        throws BackoutException
    {
        internal_list.set_val_on_key(active_event,key,to_write);
    }

    @Override
    public boolean return_internal_val_from_container()
    {
        return internal_list.return_internal_val_from_container();
    }

    @Override
    public int get_len(ActiveEvent active_event) throws BackoutException
    {
        return internal_list.get_len(active_event);
    }
    
    @Override
    public Double get_len_boxed(ActiveEvent active_event) throws BackoutException
    {
        return internal_list.get_len_boxed(active_event);
    }
    
    @Override
    public ArrayList<RalphObject<V,D>> get_iterable(ActiveEvent active_event)
        throws BackoutException
    {
        return internal_list.get_iterable(active_event);
    }

    @Override
    public void remove(ActiveEvent active_event, Integer key_to_delete)
        throws BackoutException
    {
        internal_list.remove(active_event,key_to_delete);
    }
    @Override
    public void remove(ActiveEvent active_event, Double key_to_delete)
        throws BackoutException
    {
        internal_list.remove(active_event,key_to_delete);
    }
    
    
    @Override
    public boolean contains_key_called(
        ActiveEvent active_event, Double contains_key)  throws BackoutException
    {
        return internal_list.contains_key_called(active_event,contains_key);
    }
    @Override
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, Double contains_key)  throws BackoutException
    {
        return internal_list.contains_key_called_boxed(active_event,contains_key);
    }
    @Override
    public boolean contains_key_called(
        ActiveEvent active_event, Integer contains_key)  throws BackoutException
    {
        return internal_list.contains_key_called(active_event,contains_key);
    }
    @Override
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, Integer contains_key) throws BackoutException
    {
        return internal_list.contains_key_called_boxed(active_event,contains_key);
    }

    @Override
    public boolean contains_val_called(
        ActiveEvent active_event, V contains_val) throws BackoutException
    {
        return internal_list.contains_val_called(active_event,contains_val);
    }

    @Override
    public void clear(ActiveEvent active_event) throws BackoutException
    {
        internal_list.clear(active_event);
    }

}
