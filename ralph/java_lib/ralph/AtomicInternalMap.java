package ralph;

import java.util.ArrayList;
import java.util.HashMap;
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
 * @param <D> --- The Java type of data that the internal locked
 * objects should dewaldoify into
 */
public class AtomicInternalMap<K,V,D> 
    extends AtomicObject <
    // The internal values that these are holding
    HashMap<K,RalphObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    HashMap<K,D>
    >
    implements ImmediateCommitSupplier, MapTypeDataWrapperSupplier,
               RalphInternalMapInterface<K,V,D>    
{
    // Keeps track of the map's index type.  Useful when serializing
    // and deserializing data.
    public IndexType index_type;
    
    private MapTypeDataWrapper<K,V,D> reference_type_val = null;
    private RalphInternalMap<K,V,D> internal_map = null;
    public EnsureAtomicWrapper<V,D> locked_wrapper = null;
    
    public AtomicInternalMap(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        internal_map = new RalphInternalMap<K,V,D>(ralph_globals);
    }

    
    public void init_multithreaded_map_container(
        boolean _log_changes,
        MapTypeDataWrapperFactory<K,V,D> rtdwc,
        HashMap<K,RalphObject<V,D>>init_val,
        IndexType _index_type,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        index_type = _index_type;
        locked_wrapper = _locked_wrapper;
        init_multithreaded_locked_object(
            rtdwc, _log_changes, init_val);
        internal_map.init_ralph_internal_map(
            _locked_wrapper,this,this);
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
            complete_commit(active_event);
        }
    }

    /** MapTypeDataWrapperSupplier Interface */
    @Override    
    public MapTypeDataWrapper<K,V,D> get_val_read(
        ActiveEvent active_event) throws BackoutException
    {
        MapTypeDataWrapper<K,V,D> wrapped_val =
            (MapTypeDataWrapper<K,V,D>)acquire_read_lock(active_event);
        return wrapped_val;
    }
    @Override    
    public MapTypeDataWrapper<K,V,D> get_val_write(
        ActiveEvent active_event) throws BackoutException
    {
        MapTypeDataWrapper<K,V,D> wrapped_val =
            (MapTypeDataWrapper<K,V,D>)acquire_write_lock(active_event);
        return wrapped_val;
    }


    /** RalphInternalMapInterface<K,V,D> Interface */
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
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        internal_map.serialize_as_rpc_arg(
            active_event,any_builder,is_reference);
    }

    
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException
    {
        internal_map.set_val_on_key(active_event,key,to_write);
    }
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key, RalphObject<V,D> to_write) throws BackoutException
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
