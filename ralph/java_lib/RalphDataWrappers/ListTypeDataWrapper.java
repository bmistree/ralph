package RalphDataWrappers;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import ralph.RalphObject;
import RalphExceptions.BackoutException;
import ralph.ActiveEvent;



/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 */
public class ListTypeDataWrapper<ValueType,DeltaValueType>
    extends DataWrapper<List<RalphObject<ValueType,DeltaValueType>>>
{
    private final boolean log_changes;
	
    /*
     * tracks all insertions, removals, etc. made to this reference
     * object so can send deltas across network to partners.
     * (Note: only used for log_changes data.)
     */
    private final List <ContainerOpTuple<Integer,ValueType,DeltaValueType>> change_log;

	
    public ListTypeDataWrapper(
        List<RalphObject<ValueType,DeltaValueType>> v, boolean _log_changes)
    {
        super(new ArrayList<RalphObject<ValueType,DeltaValueType>>(v));
        if (_log_changes)
            change_log = new ArrayList<ContainerOpTuple<Integer,ValueType,DeltaValueType>>();
        else
            change_log = null;
        
        log_changes = _log_changes;
    }

    /**
       Reference is unmodifiable.
     */
    public List<ContainerOpTuple<Integer,ValueType,DeltaValueType>>
        get_unmodifiable_change_log()
    {
        return Collections.unmodifiableList(change_log);
    }
    
    public ListTypeDataWrapper(
        ListTypeDataWrapper<ValueType,DeltaValueType> v, boolean _log_changes)
    {
        this(v.val,_log_changes);
    }
	
    /**
     * @param {bool} incorporating_deltas --- True if we are setting
     a value as part of incorporating deltas that were made by
     partner to log_changes data.  In this case, we do not want to log
     the changes: we do not want our partner to replay the same
     changes they already have.
        
     * @param active_event
     * @param key
     * @param to_write
     * @param incorporating_deltas
     * 
     * To ensure that we aren't copying references to value variables, we actually 
     * call get_val on to_write and use that value.
     * 
     */
    public void set_val_on_key(
        ActiveEvent active_event,Integer key,
        RalphObject<ValueType,DeltaValueType> to_write,
        boolean incorporating_deltas) throws BackoutException
    {
        if ((log_changes) && (! incorporating_deltas))
            change_log.add(write_key_tuple(key));

        val.get(key).set_val(active_event,to_write.get_val(active_event));
        return;	
    }
		
    public void set_val_on_key(
        ActiveEvent active_event,Integer key,
        RalphObject<ValueType,DeltaValueType> to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }
	
    public void del_key(
        ActiveEvent active_event, Integer key_to_delete,
        boolean incorporating_deltas)
    {
        RalphObject<ValueType,DeltaValueType> what_removed =
            val.remove(key_to_delete.intValue());
    	if (log_changes && (! incorporating_deltas))
            change_log.add(delete_key_tuple(key_to_delete,what_removed));
    }
    
    public void del_key(ActiveEvent active_event,Integer key_to_delete)
    {
    	del_key(active_event,key_to_delete,false);
    }
    
    public void append(
        ActiveEvent active_event,
        RalphObject<ValueType,DeltaValueType> new_object,
        boolean incorporating_deltas) throws BackoutException
    {
        Integer key_added = new Integer(val.size());        
    	if (log_changes && (! incorporating_deltas))
            change_log.add(add_key_tuple(key_added,new_object));
    	val.add(key_added,new_object);        
    }
    
    public void append(
        ActiveEvent active_event,
        RalphObject<ValueType,DeltaValueType> new_object)
        throws BackoutException
    {
    	append(active_event,new_object,false);
    }
    
    /**
     * Can only be called on list, so where to insert is int
     * @param active_event
     * @param where_to_insert
     * @param new_val
     * @param incorporating_deltas
     */
    public void insert(
        ActiveEvent active_event, int where_to_insert,
        RalphObject<ValueType,DeltaValueType> new_val,
        boolean incorporating_deltas)
        throws BackoutException
    {
        Integer key_added = new Integer(val.size());
        if (log_changes && (! incorporating_deltas))
            change_log.add(add_key_tuple(key_added,new_val));
    	val.add(where_to_insert,new_val);
    }
    
    public void insert(
        ActiveEvent active_event,int where_to_insert,
        RalphObject<ValueType,DeltaValueType> new_val) throws BackoutException
    {
    	insert(active_event,where_to_insert,new_val,false);
    }
    
    private ContainerOpTuple<Integer,ValueType,DeltaValueType> delete_key_tuple(
        Integer _key, RalphObject<ValueType,DeltaValueType> what_removed)
    {
        return
            new ContainerOpTuple<Integer,ValueType,DeltaValueType>(
                ContainerOpTuple.OpType.DELETE,_key,what_removed);
    }

    private ContainerOpTuple <Integer,ValueType,DeltaValueType>add_key_tuple(
        Integer _key, RalphObject<ValueType,DeltaValueType> what_added)
    {
        return
            new ContainerOpTuple<Integer,ValueType,DeltaValueType>(
                ContainerOpTuple.OpType.ADD,_key,what_added);
    }
	
    private ContainerOpTuple write_key_tuple(Integer _key)
    {
        return
            new ContainerOpTuple<Integer,ValueType,DeltaValueType>(
                ContainerOpTuple.OpType.WRITE,_key,null);
    }
}
