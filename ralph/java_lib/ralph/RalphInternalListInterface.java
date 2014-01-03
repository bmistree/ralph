package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;

/**
 * @param <V> --- The Java type of data that are elements in the list
 * @param <D> --- The Java type of data that elements should dewaldoify into.
 */
public interface RalphInternalListInterface<V,D> 
{
    public void insert(
        ActiveEvent active_event, Double index_to_insert_in,
        V what_to_insert) throws BackoutException;
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in,
        V what_to_insert) throws BackoutException;
    public void insert(
        ActiveEvent active_event, Integer key,
        RalphObject<V,D> to_insert)  throws BackoutException;

    
    public V get_val_on_key(
        ActiveEvent active_event, Double key) throws BackoutException;
    public V get_val_on_key(
        ActiveEvent active_event, Integer key) throws BackoutException;
    
    public void append(
        ActiveEvent active_event, V what_to_insert) throws BackoutException;
    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException;
    
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write) throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, Double key, V to_write) throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,D> to_write)
        throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,D> to_write)
        throws BackoutException;
    public boolean return_internal_val_from_container();

    public int get_len(ActiveEvent active_event) throws BackoutException;
    public Double get_len_boxed(ActiveEvent active_event) throws BackoutException;

    /**
       Must guarantee that will only read from the returned value, not
       write to it.
     */
    public ArrayList<RalphObject<V,D>> get_iterable(ActiveEvent active_event)
        throws BackoutException;
    
    
    public void remove(ActiveEvent active_event, Integer key_to_delete)
        throws BackoutException;
    public void remove(ActiveEvent active_event, Double key_to_delete)
        throws BackoutException;
    
    public boolean contains_key_called(
        ActiveEvent active_event, Double contains_key)  throws BackoutException;
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, Double contains_key)  throws BackoutException;

    public boolean contains_key_called(
        ActiveEvent active_event, Integer contains_key)  throws BackoutException;
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, Integer contains_key) throws BackoutException;

    
    public boolean contains_val_called(
        ActiveEvent active_event, V contains_val) throws BackoutException;
}
