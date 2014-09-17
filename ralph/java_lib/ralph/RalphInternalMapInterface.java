package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;

/**
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that are elements in the list
 */
public interface RalphInternalMapInterface<K,V,ValueDeltaType> 
{
    public V get_val_on_key(ActiveEvent active_event, K key)
        throws BackoutException;

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException;


    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, K key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException;

    /**
       Should only be called during deserialization.  Writes directly
       to internal val.  Caller must ensure no contention.
     */
    public void direct_set_val_on_key(K key, V to_write);
    public void direct_set_val_on_key(
        K key, RalphObject<V,ValueDeltaType> to_write);


    
    public boolean return_internal_val_from_container();
    
    public int get_len(ActiveEvent active_event) throws BackoutException;
    public Double get_len_boxed(ActiveEvent active_event) 
        throws BackoutException;
    
    public Set<K> get_iterable(ActiveEvent active_event)
        throws BackoutException;
    
    public void remove(ActiveEvent active_event, K key_to_delete)
        throws BackoutException;

    public boolean contains_key_called(
        ActiveEvent active_event, K contains_key)  throws BackoutException;
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, K contains_key) throws BackoutException;
    public boolean contains_val_called(
        ActiveEvent active_event,
        V contains_val) throws BackoutException;

    public void clear(ActiveEvent active_event) throws BackoutException;
}
