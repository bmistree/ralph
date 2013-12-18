package ralph;

import java.util.ArrayList;
import java.util.HashMap;

import RalphExceptions.BackoutException;


/**
 * @param <K>  ---- The keys used for indexing
 * @param <V> ---- The type of each internal value in the internal
 * hash map
 * @param <D> ---- The type that each value in the internal hash map
 * would dewaldoify into
 * 
 * An internal map of numbers to strings:
 * 
 * ContainerInterface<Number,String,String>
 * 
 * An internal map of numbers to maps of numbers to strings
 * 
 * ContainerInterface<Number
 *      LockedMap<Number, String, HashMap<Number,String> >,
 *      HashMap<Number,String>>
 * 
 * If v is a non-external waldo object, then copy v's internal val out and 
 * put in a separate locked object.
 * 
 */
public interface ContainerInterface <K,V,D>
{
    public V get_val_on_key(ActiveEvent active_event, K key)
        throws BackoutException;
	
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write,
        boolean copy_if_peered) throws BackoutException;
    
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException;
    
    public void del_key_called(
        ActiveEvent active_event,K key_to_delete) throws BackoutException;
    
    public int get_len(
        ActiveEvent active_event) throws BackoutException;

    public Double get_len_boxed(
        ActiveEvent active_event) throws BackoutException;

    public ArrayList<K> get_keys(
        ActiveEvent active_event) throws BackoutException;
    
    public boolean contains_key_called(
        ActiveEvent active_event, K contains_key) throws BackoutException;

    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, K contains_key) throws BackoutException;
    
    public boolean contains_val_called(
        ActiveEvent active_event,V contains_val) throws BackoutException;
	
}
