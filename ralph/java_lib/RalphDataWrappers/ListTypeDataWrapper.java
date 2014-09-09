package RalphDataWrappers;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ralph.RalphObject;
import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 */
public class ListTypeDataWrapper<T>
    extends DataWrapper<List<RalphObject<T>>>{
	
    public class OpTuple
    {
        public static final int DELETE_FLAG = 0;
        public static final int ADD_FLAG = 1;
        public static final int WRITE_FLAG = 2;
		
        public int type;
        public Integer key;
        public RalphObject<T> what_added_or_removed = null;
        public OpTuple(
            int _type, Integer _key, RalphObject<T> _what_added_or_removed)
        {
            type = _type;
            key = _key;
            what_added_or_removed = _what_added_or_removed;
        }		
		
    }
    private boolean log_changes;
    protected boolean has_been_written_since_last_msg = false;
	
    /*
     * tracks all insertions, removals, etc. made to this reference
     * object so can send deltas across network to partners.
     * (Note: only used for log_changes data.)
     */
    public List <OpTuple> partner_change_log = new ArrayList<OpTuple>(); 

	
    public ListTypeDataWrapper(List<RalphObject<T>> v, boolean _log_changes)
    {
        super( new ArrayList<RalphObject<T>>(v),_log_changes);
        log_changes = _log_changes;
    }
	
    public ListTypeDataWrapper(ListTypeDataWrapper<T> v, boolean _log_changes)
    {
        super(v.val,_log_changes);
        log_changes = _log_changes;
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
        ActiveEvent active_event,Integer key, RalphObject<T> to_write,
        boolean incorporating_deltas) throws BackoutException
    {
        if ((log_changes) && (! incorporating_deltas))
            partner_change_log.add(write_key_tuple(key));

        val.get(key).set_val(active_event,to_write.get_val(active_event));
        return;	
    }
		
    public void set_val_on_key(
        ActiveEvent active_event,Integer key, RalphObject<T> to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }
	
	
    public void del_key(
        ActiveEvent active_event, Integer key_to_delete,
        boolean incorporating_deltas)
    {
        RalphObject<T> what_removed =
            val.remove(key_to_delete.intValue());
    	if (log_changes && (! incorporating_deltas))
            partner_change_log.add(delete_key_tuple(key_to_delete,what_removed));
    }
    
    public void del_key(ActiveEvent active_event,Integer key_to_delete)
    {
    	del_key(active_event,key_to_delete,false);
    }
    
    public void append(
        ActiveEvent active_event, RalphObject<T> new_object,
        boolean incorporating_deltas) throws BackoutException
    {
        Integer key_added = new Integer(val.size());        
    	if (log_changes && (! incorporating_deltas))
            partner_change_log.add(add_key_tuple(key_added,new_object));
    	val.add(key_added,new_object);        
    }
    
    public void append(
        ActiveEvent active_event, RalphObject<T> new_object)
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
        RalphObject<T> new_val, boolean incorporating_deltas)
        throws BackoutException
    {
        Integer key_added = new Integer(val.size());
        if (log_changes && (! incorporating_deltas))
            partner_change_log.add(add_key_tuple(key_added,new_val));
    	val.add(where_to_insert,new_val);
    }
    
    public void insert(
        ActiveEvent active_event,int where_to_insert,
        RalphObject<T> new_val) throws BackoutException
    {
    	insert(active_event,where_to_insert,new_val,false);
    }

    public OpTuple delete_key_tuple(Integer _key, RalphObject<T> what_removed)
    {
        return new OpTuple(OpTuple.DELETE_FLAG,_key,what_removed);
    }
    public boolean is_delete_key_tuple(OpTuple opt)
    {
        return opt.type == OpTuple.DELETE_FLAG;
    }

    public OpTuple add_key_tuple(Integer _key, RalphObject<T> what_added)
    {
        return new OpTuple(OpTuple.ADD_FLAG,_key,what_added);
    }
    public boolean is_add_key_tuple(OpTuple opt)
    {
        return opt.type == OpTuple.ADD_FLAG;
    }
	
    public OpTuple write_key_tuple(Integer _key)
    {
        return new OpTuple(OpTuple.WRITE_FLAG,_key,null);
    }
    public boolean is_write_key(OpTuple opt)
    {
        return opt.type == OpTuple.WRITE_FLAG;
    }	
}
