package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import ralph.NonAtomicInternalMap.IndexType;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;
import RalphDataWrappers.MapTypeDataWrapperSupplier;

/**
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that the keys should point to.
 * @param <ValueDeltaType> --- The type of data held by the values in
 * the map that should get passed to the versionhelper.
 */
public class AtomicInternalMap<K,V,ValueDeltaType> 
    extends SpeculativeAtomicObject <
    // The internal values that these are holding
    Map<K,RalphObject<V,ValueDeltaType>>,
    VersionMapDeltas<K,V,ValueDeltaType>
    >
    implements ImmediateCommitSupplier, MapTypeDataWrapperSupplier,
        RalphInternalMapInterface<K,V,ValueDeltaType>
{
    // Keeps track of the map's index type.  Useful when serializing
    // and deserializing data.
    public NonAtomicInternalMap.IndexType index_type;
    private MapTypeDataWrapper<K,V,ValueDeltaType> reference_type_val = null;
    private RalphInternalMap<K,V,ValueDeltaType> internal_map = null;
    public EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;
    
    public AtomicInternalMap(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        internal_map = new RalphInternalMap<K,V,ValueDeltaType>(ralph_globals);
        version_helper = VersionMapDeltas.MAP_VERSION_HELPER;
    }

    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            Map<K,RalphObject<V,ValueDeltaType>>,
            VersionMapDeltas<K,V,ValueDeltaType>>
        // function name and arguments
        duplicate_for_speculation(Map<K,RalphObject<V,ValueDeltaType>> to_speculate_on)
    {
        AtomicInternalMap <K,V,ValueDeltaType> to_return =
            new AtomicInternalMap(ralph_globals);
        to_return.set_derived(this);
        to_return.init_multithreaded_map_container(
            log_changes,
            (MapTypeDataWrapperFactory<K,V,ValueDeltaType>)data_wrapper_constructor,
            to_speculate_on,
            index_type,locked_wrapper);

        return to_return;
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
        if (ralph_globals.local_version_manager == null)
            return;

        MapTypeDataWrapper<K,V,ValueDeltaType> map_dirty_val =
            (MapTypeDataWrapper<K,V,ValueDeltaType>) dirty_val;
        
        VersionMapDeltas<K,V,ValueDeltaType> deltas =
            new VersionMapDeltas<K,V,ValueDeltaType>(
                map_dirty_val.get_unmodifiable_change_log());
        version_helper.save_version(
            uuid, deltas,active_event.commit_metadata);
    }

    
    public void init_multithreaded_map_container(
        boolean _log_changes,
        MapTypeDataWrapperFactory<K,V,ValueDeltaType> rtdwc,
        Map<K,RalphObject<V,ValueDeltaType>>init_val,
        NonAtomicInternalMap.IndexType _index_type,
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper)
    {
        index_type = _index_type;
        locked_wrapper = _locked_wrapper;
        init_multithreaded_locked_object(
            rtdwc, version_helper,_log_changes, init_val);
        internal_map.init_ralph_internal_map(
            _locked_wrapper,this,this,_index_type);
    }

    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        Util.logger_assert(
            "Must fill in write_if_different in AtomicInternalMap.");
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
            active_event.update_commit_metadata();
            complete_commit(active_event);
        }
    }

    /** MapTypeDataWrapperSupplier Interface */
    @Override    
    public MapTypeDataWrapper<K,V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val =
            (MapTypeDataWrapper<K,V,ValueDeltaType>)
            acquire_read_lock(active_event);
        return wrapped_val;
    }
    @Override    
    public MapTypeDataWrapper<K,V,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        MapTypeDataWrapper<K,V,ValueDeltaType> wrapped_val =
            (MapTypeDataWrapper<K,V,ValueDeltaType>)
            acquire_write_lock(active_event);
        return wrapped_val;
    }


    /** RalphInternalMapInterface<K,V> Interface */
    @Override
    public V get_val_on_key(ActiveEvent active_event, K key)
        throws BackoutException
    {
        return internal_map.get_val_on_key(active_event,key);
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
        internal_map.serialize_as_rpc_arg(active_event,any_builder);
        any_builder.setIsTvar(true);
    }

    
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException
    {
        internal_map.set_val_on_key(active_event,key,to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key,
        RalphObject<V,ValueDeltaType> to_write) throws BackoutException
    {
        internal_map.set_val_on_key(active_event,key,to_write);
    }

    @Override
    public boolean return_internal_val_from_container()
    {
        return internal_map.return_internal_val_from_container();
    }
    
    @Override
    public int get_len(ActiveEvent active_event) throws BackoutException
    {
        return internal_map.get_len(active_event);
    }
    @Override
    public Double get_len_boxed(ActiveEvent active_event) 
        throws BackoutException
    {
        return internal_map.get_len_boxed(active_event);
    }

    @Override
    public Set<K> get_iterable(ActiveEvent active_event)
        throws BackoutException
    {
        return internal_map.get_iterable(active_event);
    }
    

    @Override
    public void remove(ActiveEvent active_event, K key_to_delete)
        throws BackoutException
    {
        internal_map.remove(active_event,key_to_delete);
    }

    @Override
    public boolean contains_key_called(
        ActiveEvent active_event, K contains_key)  throws BackoutException
    {
        return internal_map.contains_key_called(active_event,contains_key);
    }

    @Override
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, K contains_key) throws BackoutException
    {
        return internal_map.contains_key_called_boxed(
            active_event,contains_key);
    }    
    
    @Override
    public boolean contains_val_called(
        ActiveEvent active_event, V contains_val) throws BackoutException
    {
        return internal_map.contains_val_called(active_event,contains_val);
    }

    @Override
    public void clear(ActiveEvent active_event) throws BackoutException
    {
        internal_map.clear(active_event);
    }
}
