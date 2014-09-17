package ralph;

import java.util.List;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperSupplier;
import ralph.AtomicList.AdditionalAtomicListSerializationContents;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

/**
 * @param <V> --- The Java type of data that the values should point
 * to.
 * @param <DeltaType> --- The type that should be passed to the
 * version helper for each internal value.  NOT for the entire list.
 */
public class NonAtomicInternalList<V,ValueDeltaType> 
    extends NonAtomicObject <
    // The internal values that these are holding
    List<RalphObject<V,ValueDeltaType>>,
    // The type that gets passed to internal version helper.
    VersionContainerDeltas
    >
    implements ImmediateCommitSupplier, ListTypeDataWrapperSupplier,
        RalphInternalListInterface<V,ValueDeltaType>
{    
    private ListTypeDataWrapper<V,ValueDeltaType> reference_type_val = null;
    private RalphInternalList<V,ValueDeltaType> internal_list = null;
    public EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;
    private final Class<V> value_type_class;
    
    public NonAtomicInternalList(
        RalphGlobals ralph_globals,
        ListTypeDataWrapperFactory<V,ValueDeltaType> ltdwf,
        List<RalphObject<V,ValueDeltaType>>init_val,
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper)
    {
        super(ralph_globals);
        value_type_class = ltdwf.value_type_class;
        
        version_helper = BaseTypeVersionHelpers.INTERNAL_LIST_TYPE_VERSION_HELPER;
        internal_list = new RalphInternalList<V,ValueDeltaType>(ralph_globals);

        locked_wrapper = _locked_wrapper;
        internal_list.init_ralph_internal_list(
            _locked_wrapper,this,this);
        
        reference_type_val =
            (ListTypeDataWrapper<V,ValueDeltaType>)ltdwf.construct(init_val, false); 
        val = reference_type_val;
    }

    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event,Object additional_serialization_contents)
    {
        String value_type_name = null;
        if (additional_serialization_contents == null)
            value_type_name = value_type_class.getName();
        else
        {
            AdditionalAtomicListSerializationContents add_ser_contents =
                (AdditionalAtomicListSerializationContents)
                additional_serialization_contents;
            value_type_name = add_ser_contents.val_class_name;
        }

        ObjectContents.InternalList.Builder internal_list_builder =
            ObjectContents.InternalList.newBuilder();
        internal_list_builder.setValTypeClassName(value_type_name);

        ObjectContents.Builder to_return = ObjectContents.newBuilder();
        to_return.setInternalListType(internal_list_builder);
        to_return.setUuid(uuid());
        to_return.setAtomic(false);
        return to_return.build();
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
        // empty method: do not commit on non-atomics.
    }

    /** ListTypeDataWrapperSupplier Interface */
    @Override    
    public ListTypeDataWrapper<V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        // do not need to acquire read lock: non-atomic
        return reference_type_val;
    }
    @Override    
    public ListTypeDataWrapper<V,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        // do not need to acquire write lock: non-atomic
        return reference_type_val;
    }

    /** RalphInternalListInterface<V> Interface */
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
        RalphObject<V,ValueDeltaType> to_insert)  throws BackoutException
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
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        internal_list.serialize_as_rpc_arg(active_event,any_builder);
        any_builder.setIsTvar(false);
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
        ActiveEvent active_event, Integer key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException
    {
        internal_list.set_val_on_key(active_event,key,to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,ValueDeltaType> to_write)
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
    public List<RalphObject<V,ValueDeltaType>> get_iterable(ActiveEvent active_event)
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
