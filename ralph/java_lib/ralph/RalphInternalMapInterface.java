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
 * @param <D> --- The Java type of data that elements should dewaldoify into.
 */
public interface RalphInternalMapInterface<K,V,D> 
{
    public V get_val_on_key(ActiveEvent active_event, K key)
        throws BackoutException;

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException;


    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, K key, RalphObject<V,D> to_write)
        throws BackoutException;
    
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
}
