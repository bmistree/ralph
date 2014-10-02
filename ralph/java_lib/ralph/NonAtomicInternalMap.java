package ralph;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;
import RalphDataWrappers.MapTypeDataWrapperSupplier;

import RalphVersions.IReconstructionContext;
import RalphVersions.ObjectHistory;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;

/**
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that the keys should point to.
 * @param <ValueDeltaType> --- The type of data held by the values in
 * the map that should get passed to the versionhelper.
 */
public class NonAtomicInternalMap<K,V,ValueDeltaType> 
    extends NonAtomicObject <
    // The internal values that these are holding
    Map<K,RalphObject<V,ValueDeltaType>>,
    VersionContainerDeltas
    >
    implements ImmediateCommitSupplier, MapTypeDataWrapperSupplier,
        RalphInternalMapInterface<K,V,ValueDeltaType>
{
    public enum IndexType{
        DOUBLE,STRING,BOOLEAN
    };

    private MapTypeDataWrapper<K,V,ValueDeltaType> reference_type_val = null;
    private RalphInternalMap<K,V,ValueDeltaType> internal_map = null;
    public EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;
    // Keeps track of the map's index type.  Useful when serializing
    // and deserializing data.
    public IndexType index_type;
    
    public NonAtomicInternalMap(
        RalphGlobals ralph_globals,
        VersionHelper<VersionContainerDeltas> internal_version_helper,
        MapTypeDataWrapperFactory<K,V,ValueDeltaType> mtdwf,
        Map<K,RalphObject<V,ValueDeltaType>>init_val,
        IndexType _index_type,
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper
        )
    {
        super(ralph_globals);
        version_helper = internal_version_helper;
        internal_map = new RalphInternalMap<K,V,ValueDeltaType>(ralph_globals);

        index_type = _index_type;
        
        locked_wrapper = _locked_wrapper;
        reference_type_val =
            (MapTypeDataWrapper<K, V,ValueDeltaType>)mtdwf.construct(init_val, false);
        val = reference_type_val;
        internal_map.init_ralph_internal_map(
            _locked_wrapper,this,this,index_type);
    }

    @Override
    public void replay (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until)
    {
        ObjectHistory.replay_internal_map(
            this,obj_history,to_play_until,reconstruction_context);
    }

    
    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event,Object additional_serialization_contents)
    {
        // FIXME: May eventually want to fill this in (eg., if
        // replacing serialization code.  Currently, it's unnecessary
        // because we just use this method for serializing version
        // histories.
        Util.logger_assert(
            "FIXME: currently, disallowing direct " +
            "serialization of nonatomicinternalmap.");
        return null;
    }
    
    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        Util.logger_assert(
            "Still must define swap method for NonAtomicInternalMap.");
    }
    
    /** ImmediateCommitSupplier interface*/
    @Override
    public void check_immediate_commit(ActiveEvent active_event)
        throws BackoutException
    {
        // empty method: do not commit on non-atomics.
    }

    /** MapTypeDataWrapperSupplier Interface */
    @Override    
    public MapTypeDataWrapper<K,V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        // do not need to acquire read lock: non-atomic
        return reference_type_val;
    }
    @Override    
    public MapTypeDataWrapper<K,V,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        // do not need to acquire write lock: non-atomic
        return reference_type_val;
    }

    /**
       Returns authoritative internal value.  Caller must ensure no
       read-write conflicts.  Mostly should be used for
       deserialization.
     */
    @Override
    public MapTypeDataWrapper<K,V,ValueDeltaType> direct_get_val()
    {
        // subtype of maptypedatawrapper supplier
        return reference_type_val;
    }
    
    
    /** RalphInternalMapInterface<K,V> Interface */
    @Override
    public V get_val_on_key(ActiveEvent active_event, K key) throws BackoutException
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
        any_builder.setIsTvar(false);
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
    public void direct_set_val_on_key(K key, V to_write)
    {
        internal_map.direct_set_val_on_key(key,to_write);
    }
    @Override
    public void direct_set_val_on_key(
        K key, RalphObject<V,ValueDeltaType> to_write)
    {
        internal_map.direct_set_val_on_key(key,to_write);
    }
    @Override
    public void direct_remove_val_on_key(K key)
    {
        internal_map.direct_remove_val_on_key(key);
    }
    @Override
    public void direct_clear()
    {
        internal_map.direct_clear();
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
