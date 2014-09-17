package ralph;

import java.util.List;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;

/**
 * @param <V> --- The Java type of data that are elements in the list
 */
public interface RalphInternalListInterface<V,ValueDeltaType> 
{
    public void insert(
        ActiveEvent active_event, Double index_to_insert_in,
        V what_to_insert) throws BackoutException;
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in,
        V what_to_insert) throws BackoutException;
    public void insert(
        ActiveEvent active_event, Integer key,
        RalphObject<V,ValueDeltaType> to_insert)  throws BackoutException;

    
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
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException;
    
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write) throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, Double key, V to_write) throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException;
    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,ValueDeltaType> to_write)
        throws BackoutException;
    public boolean return_internal_val_from_container();

    /**
       Direct operations are used during deserialization.  Caller must
       ensure no read-write conflicts.
     */
    public void direct_append(V what_to_insert);
    public void direct_append(RalphObject<V,ValueDeltaType> what_to_insert);
    public void direct_set_val_on_key(Integer key, V to_write);
    public void direct_set_val_on_key(
        Integer key, RalphObject<V,ValueDeltaType> to_write);

    
    public int get_len(ActiveEvent active_event) throws BackoutException;
    public Double get_len_boxed(ActiveEvent active_event) throws BackoutException;

    /**
       Must guarantee that will only read from the returned value, not
       write to it.
     */
    public List<RalphObject<V,ValueDeltaType>> get_iterable(ActiveEvent active_event)
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

    public void clear(ActiveEvent active_event) throws BackoutException;
}
