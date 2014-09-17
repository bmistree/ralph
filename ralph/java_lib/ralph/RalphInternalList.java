package ralph;

import java.util.List;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperSupplier;

/**
 * @param <V> --- The Java type of data that are elements in the list
 */
public class RalphInternalList<V,ValueDeltaType>
    implements RalphInternalListInterface<V,ValueDeltaType>
{
    private EnsureAtomicWrapper<V,ValueDeltaType>locked_wrapper;
    private ListTypeDataWrapperSupplier<V,ValueDeltaType> data_wrapper_supplier;
    private ImmediateCommitSupplier immediate_commit_supplier;

    private RalphGlobals ralph_globals = null;

    public RalphInternalList(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
    }
    
    public void init_ralph_internal_list(
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper,
        ListTypeDataWrapperSupplier<V,ValueDeltaType>_data_wrapper_supplier,
        ImmediateCommitSupplier _immediate_commit_supplier)
    {
        locked_wrapper = _locked_wrapper;
        data_wrapper_supplier = _data_wrapper_supplier;
        immediate_commit_supplier = _immediate_commit_supplier;
    }

    private ListTypeDataWrapper<V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        return data_wrapper_supplier.get_val_read(active_event);
    }
    private ListTypeDataWrapper<V,ValueDeltaType> get_val_write(
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
        RalphObject<V,ValueDeltaType> wrapped_to_insert =
            locked_wrapper.ensure_atomic_object(what_to_insert,ralph_globals);
        insert(active_event,index_to_insert_in,wrapped_to_insert);
    }
    @Override
    public void insert(
        ActiveEvent active_event, Integer key,
        RalphObject<V,ValueDeltaType> to_insert)  throws BackoutException
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_write(active_event);
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
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        RalphObject<V,ValueDeltaType> internal_key_val = wrapped_val.val.get(key);

        Object to_return = null;        
        if (internal_key_val.return_internal_val_from_container())
            to_return = internal_key_val.get_val(active_event);
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
        RalphObject<V,ValueDeltaType> wrapped_to_insert =
            locked_wrapper.ensure_atomic_object(what_to_insert,ralph_globals);

        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            get_val_write(active_event);
        int size = wrapped_val.val.size();
        Integer index_to_insert_in = new Integer(size);
        insert(
            active_event, index_to_insert_in, wrapped_to_insert);
    }

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.  Note: parent that calls will set
       is_tvar field.
     */
    @Override
    public void serialize_as_rpc_arg (
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        // mostly useless
        any_builder.setVarName("");

        // get list's internal data
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        List<RalphObject<V,ValueDeltaType>> ralph_list = wrapped_val.val;

        // create a list message that contains all changes
        VariablesProto.Variables.List.Builder list_builder =
            VariablesProto.Variables.List.newBuilder();

        for (RalphObject<V,ValueDeltaType> to_append : ralph_list)
        {
            VariablesProto.Variables.Any.Builder single_element_builder =
                VariablesProto.Variables.Any.newBuilder();
            to_append.serialize_as_rpc_arg(
                active_event,single_element_builder);
            list_builder.addListValues(single_element_builder);
        }
        list_builder.setElementTypeIdentifier(
            locked_wrapper.get_serialization_label());
        any_builder.setList(list_builder);
        
        // non-atomics should not hold locks
        check_immediate_commit(active_event);
    }
    
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write) throws BackoutException
    {
        RalphObject<V,ValueDeltaType> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(to_write,ralph_globals);
        set_val_on_key(active_event,key,wrapped_to_write);
    }
    
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key.intValue(),to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException 
    {
        set_val_on_key(active_event,new Integer(key.intValue()),to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException 
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_write(active_event);
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
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_read(active_event);
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
    public List<RalphObject<V,ValueDeltaType>> get_iterable(ActiveEvent active_event)
        throws BackoutException
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        check_immediate_commit(active_event);
        return wrapped_val.val;
    }
    
    @Override
    public void remove(ActiveEvent active_event, Integer key_to_delete)
        throws BackoutException
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_write(active_event);
        wrapped_val.del_key(active_event, key_to_delete);
        check_immediate_commit(active_event);
    }
    @Override
    public void remove(ActiveEvent active_event, Double key_to_delete)
        throws BackoutException
    {
        remove(active_event,key_to_delete.intValue());
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
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_read(active_event);
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
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_read(active_event);
        Util.logger_assert(
            "FIXME: must fill in contains_val_called for lists.");
        check_immediate_commit(active_event);
        return false;
    }

    @Override
    public void clear(ActiveEvent active_event) throws BackoutException
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val = get_val_write(active_event);
        wrapped_val.val.clear();
        check_immediate_commit(active_event);
    }


    /**
       Direct operations are used during deserialization.  Caller must
       ensure no read-write conflicts.
     */
    @Override
    public void direct_append(V what_to_insert)
    {
        RalphObject<V,ValueDeltaType> wrapped_to_insert =
            locked_wrapper.ensure_atomic_object(
                what_to_insert,ralph_globals);
        
        direct_append(wrapped_to_insert);
    }
    @Override
    public void direct_append(RalphObject<V,ValueDeltaType> what_to_insert)
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        wrapped_val.val.add(what_to_insert);
    }
    @Override
    public void direct_set_val_on_key(Integer key, V to_write)
    {
        RalphObject<V,ValueDeltaType> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(
                to_write,ralph_globals);
        direct_set_val_on_key(key,wrapped_to_write);
    }
    
    @Override
    public void direct_set_val_on_key(
        Integer key, RalphObject<V,ValueDeltaType> to_write)
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        wrapped_val.val.set(key,to_write);
    }

    @Override
    public void direct_remove(Integer key_to_delete)
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        wrapped_val.val.remove(key_to_delete.intValue());
    }
    @Override
    public void direct_clear()
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            data_wrapper_supplier.direct_get_val();
        wrapped_val.val.clear();
    }
}
