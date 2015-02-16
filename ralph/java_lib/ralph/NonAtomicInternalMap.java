package ralph;

import java.util.Map;
import java.util.Set;

import RalphExceptions.BackoutException;

import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;
import RalphDataWrappers.MapTypeDataWrapperSupplier;

import RalphVersions.IReconstructionContext;
import RalphVersions.ObjectHistory;

import ralph.AtomicMap.AdditionalAtomicMapSerializationContents;

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
    private MapTypeDataWrapper<K,V,ValueDeltaType> reference_type_val = null;
    private RalphInternalMap<K,V,ValueDeltaType> internal_map = null;
    public EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;

    private final Class<K> key_type_class;
    private final Class<V> value_type_class;
    
    public NonAtomicInternalMap(
        RalphGlobals ralph_globals,
        VersionHelper<VersionContainerDeltas> internal_version_helper,
        MapTypeDataWrapperFactory<K,V,ValueDeltaType> mtdwf,
        Map<K,RalphObject<V,ValueDeltaType>>init_val,
        EnsureAtomicWrapper<V,ValueDeltaType>_locked_wrapper
        )
    {
        super(ralph_globals);
        version_helper = internal_version_helper;
        internal_map = new RalphInternalMap<K,V,ValueDeltaType>(ralph_globals);

        locked_wrapper = _locked_wrapper;
        key_type_class = mtdwf.key_type_class;
        value_type_class = mtdwf.value_type_class;
        
        reference_type_val =
            (MapTypeDataWrapper<K, V,ValueDeltaType>)mtdwf.construct(init_val, false);
        val = reference_type_val;
        internal_map.init_ralph_internal_map(_locked_wrapper,this,this);
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
    public void deserialize(
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until, ActiveEvent act_event)
        throws BackoutException
    {
        ObjectHistory.deserialize_internal_map(
            this,obj_history,to_play_until,reconstruction_context,act_event);
    }

    

    @Override
    public ObjectContents serialize_contents(
        ActiveEvent active_event,Object additional_serialization_contents,
        SerializationContext serialization_context) throws BackoutException
    {
        // FIXME: should rename
        // AdditionalAtomicMapSerializationContents to be specific to
        // maps, not atomic maps.
        
        String key_type_name = null;
        String value_type_name = null;
        if (additional_serialization_contents == null)
        {
            key_type_name = key_type_class.getName();
            value_type_name = value_type_class.getName();
        }
        else
        {
            AdditionalAtomicMapSerializationContents add_ser_contents =
                (AdditionalAtomicMapSerializationContents)
                additional_serialization_contents;
            key_type_name = add_ser_contents.key_class_name;
            value_type_name = add_ser_contents.val_class_name;
        }

        return RalphInternalMap.<K,V,ValueDeltaType>serialize_contents(
            active_event,key_type_name,value_type_name,serialization_context,
            false,uuid(),this,this);
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
