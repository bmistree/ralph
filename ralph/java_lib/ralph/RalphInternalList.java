package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperSupplier;

/**
 * @param <V> --- The Java type of data that are elements in the list
 * @param <D> --- The Java type of data that elements should dewaldoify into.
 */
public class RalphInternalList<V,D>
    implements RalphInternalListInterface<V,D>
{
    private EnsureAtomicWrapper<V,D>locked_wrapper;
    private ListTypeDataWrapperSupplier<V,D> data_wrapper_supplier;
    private ImmediateCommitSupplier immediate_commit_supplier;
    
    public void init_ralph_internal_list(
        EnsureAtomicWrapper<V,D>_locked_wrapper,
        ListTypeDataWrapperSupplier<V,D>_data_wrapper_supplier,
        ImmediateCommitSupplier _immediate_commit_supplier)
    {
        locked_wrapper = _locked_wrapper;
        data_wrapper_supplier = _data_wrapper_supplier;
        immediate_commit_supplier = _immediate_commit_supplier;
    }

    private ListTypeDataWrapper<V,D> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        return data_wrapper_supplier.get_val_read(active_event);
    }
    private ListTypeDataWrapper<V,D> get_val_write(
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
    public void insert(
        ActiveEvent active_event, Double index_to_insert_in,
        V what_to_insert)
        throws BackoutException
    {
        insert(
            active_event,index_to_insert_in.intValue(),what_to_insert);
    }
    
    @Override
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in,
        V what_to_insert)
        throws BackoutException
    {
        RalphObject<V,D> wrapped_to_insert =
            locked_wrapper.ensure_atomic_object(what_to_insert);
        insert(active_event,index_to_insert_in,wrapped_to_insert);
    }
    @Override
    public void insert(
        ActiveEvent active_event, Integer key,
        RalphObject<V,D> to_insert)  throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val = get_val_write(active_event);
        wrapped_val.insert(active_event,key,to_insert);
        check_immediate_commit(active_event);
    }
    
    @Override
    public V get_val_on_key(
        ActiveEvent active_event, Double key) throws BackoutException
    {
        return get_val_on_key(
            active_event,new Integer(key.intValue()));
    }
    @Override
    public V get_val_on_key(
        ActiveEvent active_event, Integer key) throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val = get_val_read(active_event);
        RalphObject<V,D> internal_key_val = wrapped_val.val.get(key);

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
        check_immediate_commit(active_event);
        return (V)to_return;
    }

    @Override
    public void append(
        ActiveEvent active_event, V what_to_insert)
        throws BackoutException
    {
        RalphObject<V,D> wrapped_to_insert =
            locked_wrapper.ensure_atomic_object(what_to_insert);

        ListTypeDataWrapper<V,D> wrapped_val =
            get_val_write(active_event);
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
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
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
        ActiveEvent active_event, Double key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key.intValue(),to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        RalphObject<V,D> wrapped_to_write = 
            locked_wrapper.ensure_atomic_object(to_write);
        set_val_on_key(active_event,key,wrapped_to_write,copy_if_peered);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,D> to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,D> to_write)
        throws BackoutException 
    {
        set_val_on_key(active_event,new Integer(key.intValue()),to_write);
    }
    @Override    
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,D> to_write,
        boolean copy_if_peered) throws BackoutException 
    {
        ListTypeDataWrapper<V,D> wrapped_val = get_val_write(active_event);
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
        ListTypeDataWrapper<V,D> wrapped_val = get_val_read(active_event);
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
    public ArrayList<Integer> get_keys(ActiveEvent active_event)
        throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val = get_val_read(active_event);
        Util.logger_assert(
            "FIXME: should be returning RalphObject of lists.");
        return null;
    }
    
    @Override
    public void del_key_called(ActiveEvent active_event, Integer key_to_delete)
        throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val = get_val_write(active_event);
        wrapped_val.del_key(active_event, key_to_delete);
        check_immediate_commit(active_event);
    }
    
    @Override
    public boolean contains_key_called(
        ActiveEvent active_event,
        Double contains_key)  throws BackoutException
    {
        return contains_key_called(
            active_event, new Integer(contains_key.intValue()));
    }
    @Override
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event,
        Double contains_key)  throws BackoutException
    {
        return contains_key_called_boxed(
            active_event, new Integer(contains_key.intValue()));
    }
    @Override
    public boolean contains_key_called(
        ActiveEvent active_event,
        Integer contains_key)  throws BackoutException
    {
        ListTypeDataWrapper<V,D> wrapped_val = get_val_read(active_event);
        int internal_val = contains_key.intValue();
        int list_size = wrapped_val.val.size();

        boolean to_return = internal_val < list_size;
        check_immediate_commit(active_event);
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
        ListTypeDataWrapper<V,D> wrapped_val = get_val_read(active_event);
        Util.logger_assert(
            "FIXME: must fill in contains_val_called for lists.");
        check_immediate_commit(active_event);
        return false;
    }
}
