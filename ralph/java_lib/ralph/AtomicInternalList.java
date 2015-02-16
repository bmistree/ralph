package ralph;

import java.util.List;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperSupplier;
import ralph.AtomicList.AdditionalAtomicListSerializationContents;

import RalphVersions.IReconstructionContext;
import RalphVersions.ObjectHistory;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;

/**
 * @param <V> --- The Java type of data that are elements in the list
 *
 * @param <ValueDeltaType> --- The type that should be passed to the
 * version helper for each internal value.  NOT for the entire list.
 */
public class AtomicInternalList<V,ValueDeltaType> 
    extends SpeculativeAtomicObject <
    // The internal values that these are holding
    List<RalphObject<V,ValueDeltaType>>,
    VersionContainerDeltas
    >
    implements ImmediateCommitSupplier,
        ListTypeDataWrapperSupplier<V,ValueDeltaType>,
        RalphInternalListInterface<V,ValueDeltaType>
{
    private RalphInternalList<V,ValueDeltaType> internal_list = null;
    public EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;
    private final Class<V> value_type_class;
    
    public AtomicInternalList(
        RalphGlobals ralph_globals,
        boolean _log_changes,
        ListTypeDataWrapperFactory<V,ValueDeltaType> ltdwf,
        List<RalphObject<V,ValueDeltaType>>init_val,
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper)
    {
        super(ralph_globals);
        internal_list = new RalphInternalList<V,ValueDeltaType>(ralph_globals);
        version_helper = BaseTypeVersionHelpers.INTERNAL_LIST_TYPE_VERSION_HELPER;

        locked_wrapper = _locked_wrapper;
        init_multithreaded_locked_object(
            ltdwf,version_helper,_log_changes, init_val,
            // For now, passing no additional serialization arguments
            // into atomic object.
            new AtomicList.AdditionalAtomicListSerializationContents(
                ltdwf.value_type_class.getName()));

        value_type_class = ltdwf.value_type_class;
        internal_list.init_ralph_internal_list(
            _locked_wrapper,this,this);
    }

    /**
       Log completed commit, if ralph globals designates to.
     */
    @Override
    public void complete_write_commit_log(
        ActiveEvent active_event)
    {
        RalphGlobals ralph_globals = active_event.get_ralph_globals();
        
        // do not do anything
        if (VersioningInfo.instance.version_saver == null)
            return;

        ListTypeDataWrapper<V,ValueDeltaType> list_dirty_val =
            (ListTypeDataWrapper<V,ValueDeltaType>) dirty_val;
        
        VersionContainerDeltas<Integer,V,ValueDeltaType> deltas =
            new VersionContainerDeltas<Integer,V,ValueDeltaType>(
                list_dirty_val.get_unmodifiable_change_log());
        version_helper.save_version(
            uuid, deltas,active_event.commit_metadata);
    }

    @Override
    public void replay (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until)
    {
        ObjectHistory.replay_internal_list(
            this,obj_history,to_play_until,reconstruction_context);
    }

    @Override
    public void deserialize (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until, ActiveEvent act_event)
        throws BackoutException
    {
        ObjectHistory.deserialize_internal_list(
            this,obj_history,to_play_until,reconstruction_context,
            act_event);
    }

    
    
    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            List<RalphObject<V,ValueDeltaType>>,
            VersionContainerDeltas>
        // function name and arguments
        duplicate_for_speculation(List<RalphObject<V,ValueDeltaType>> to_speculate_on)
    {
        AtomicInternalList<V,ValueDeltaType> to_return = 
            new AtomicInternalList(
                ralph_globals,
                log_changes,
                (ListTypeDataWrapperFactory<V,ValueDeltaType>)data_wrapper_constructor,
                to_speculate_on,
                locked_wrapper);
        to_return.set_derived(this);

        return to_return;
    }

    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event,Object additional_serialization_contents,
        SerializationContext serialization_context) throws BackoutException
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

        return RalphInternalList.<V,ValueDeltaType>serialize_contents(
            active_event,value_type_name,serialization_context,true,
            uuid(),this,this);
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
            if (active_event != null)
                active_event.update_commit_metadata();
            complete_commit(active_event);
        }
    }

    
    /** ListTypeDataWrapperSupplier Interface */
    @Override    
    public ListTypeDataWrapper<V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            (ListTypeDataWrapper<V,ValueDeltaType>)acquire_read_lock(active_event);
        return wrapped_val;
    }
    @Override    
    public ListTypeDataWrapper<V,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        ListTypeDataWrapper<V,ValueDeltaType> wrapped_val =
            (ListTypeDataWrapper<V,ValueDeltaType>)acquire_write_lock(active_event);
        return wrapped_val;
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
    public void append(
        ActiveEvent active_event, RalphObject<V,ValueDeltaType> what_to_insert)
        throws BackoutException
    {
        internal_list.append(active_event,what_to_insert);
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
        ActiveEvent active_event, Integer key,
        RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException
    {
        internal_list.set_val_on_key(active_event,key,to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Double key,
        RalphObject<V,ValueDeltaType> to_write)
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
    public List<RalphObject<V,ValueDeltaType>> get_iterable(
        ActiveEvent active_event) throws BackoutException
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

    /**
       Returns authoritative internal value.  Caller must ensure no
       read-write conflicts.  Mostly should be used for
       deserialization.
     */
    @Override
    public ListTypeDataWrapper<V,ValueDeltaType> direct_get_val()
    {
        return (ListTypeDataWrapper<V,ValueDeltaType>)val;
    }
    
    /**
       Direct operations are used during deserialization.  Caller must
       ensure no read-write conflicts.
     */
    @Override
    public void direct_append(V what_to_insert)
    {
        internal_list.direct_append(what_to_insert);
    }
    @Override
    public void direct_append(RalphObject<V,ValueDeltaType> what_to_insert)
    {
        internal_list.direct_append(what_to_insert);
    }
    @Override
    public void direct_set_val_on_key(Integer key, V to_write)
    {
        internal_list.direct_set_val_on_key(key,to_write);
    }
    @Override
    public void direct_set_val_on_key(
        Integer key, RalphObject<V,ValueDeltaType> to_write)
    {
        internal_list.direct_set_val_on_key(key,to_write);
    }

    @Override
    public void direct_remove(Integer key_to_delete)
    {
        internal_list.direct_remove(key_to_delete);
    }
    @Override
    public void direct_clear()
    {
        internal_list.direct_clear();
    }
    
}
