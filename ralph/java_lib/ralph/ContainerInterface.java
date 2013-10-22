package ralph;

import java.util.ArrayList;
import java.util.HashMap;

import RalphExceptions.BackoutException;

import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleInternalListDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleInternalMapDelta;


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
    public V get_val_on_key(LockedActiveEvent active_event, K key);
	
    public void set_val_on_key(
        LockedActiveEvent active_event, K key, V to_write,
        boolean copy_if_peered) throws BackoutException;
    
    public void set_val_on_key(
        LockedActiveEvent active_event, K key, V to_write) throws BackoutException;
    
    public void del_key_called(
        LockedActiveEvent active_event,K key_to_delete) throws BackoutException;
    
    public int get_len(
        LockedActiveEvent active_event) throws BackoutException;
    
    public ArrayList<K> get_keys(
        LockedActiveEvent active_event) throws BackoutException;
    
    public boolean contains_key_called(
        LockedActiveEvent active_event, K contains_key) throws BackoutException;
    
    public boolean contains_val_called(
        LockedActiveEvent active_event,V contains_val) throws BackoutException;
	
    /**
     * @see waldoLockedObj.waldoLockedObj
     * @param active_event
     * @return
     */
    public DataWrapper<HashMap<K,V>, HashMap<K,D>>
        get_dirty_wrapped_val(LockedActiveEvent active_event);

    /**
     *@param {SingleListDelta or SingleMapDelta} delta_to_incorporate

     @param {SingleInternalListDelta or SingleInternalMapDelta}
     delta_to_incorporate
		
     When a peered or sequence peered container (ie, map, list, or
     struct) is modified by one endpoint, those changes must be
     reflected on the other endpoint.  This method takes the
     changes that one endpoint has made on a container, represented
     by delta_to_incorporate, and applies them (if we can).
    */
    public void incorporate_deltas(
        SingleInternalListDelta delta_to_incorporate, LockedActiveEvent active_event);
    public void incorporate_deltas(
        SingleInternalMapDelta delta_to_incorporate, LockedActiveEvent active_event);
	    		
}
