package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import RalphExceptions.BackoutException;

/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 * @param <D> --- What the java variables in the hashmap should
 * dewaldoify into (if they are locked objects)
 */
public class ListTypeDataWrapper<T,D>
    extends DataWrapper<ArrayList<RalphObject<T,D>>, ArrayList<D>>{
	
    class OpTuple
    {
        public static final int DELETE_FLAG = 0;
        public static final int ADD_FLAG = 1;
        public static final int WRITE_FLAG = 2;
		
        public int type;
        public Integer key;
        public OpTuple(int _type, Integer _key)
        {
            type = _type;
            key = _key;
        }		
		
    }
    private boolean peered;
    protected boolean has_been_written_since_last_msg = false;
	
    /*
     * tracks all insertions, removals, etc. made to this reference
     * object so can send deltas across network to partners.
     * (Note: only used for peered data.)
     */
    private ArrayList <OpTuple> partner_change_log = new ArrayList<OpTuple>(); 

	
    public ListTypeDataWrapper(ArrayList<RalphObject<T,D>> v, boolean _peered)
    {
        super( (ArrayList<RalphObject<T,D>>)v.clone(),_peered);
        peered = _peered;
    }
	
    public ListTypeDataWrapper(ListTypeDataWrapper<T,D> v, boolean _peered)
    {
        super(v.val,_peered);
        peered = _peered;
    }
	
    public ArrayList<D> de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        ArrayList<D>to_return_list = new ArrayList<D>();
        for (RalphObject<T,D> locked_obj : val)
            to_return_list.add(locked_obj.de_waldoify(active_event) );
        return to_return_list;        
    }
	
	
    /**
     * @param {bool} incorporating_deltas --- True if we are setting
     a value as part of incorporating deltas that were made by
     partner to peered data.  In this case, we do not want to log
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
        ActiveEvent active_event,Integer key, RalphObject<T,D> to_write,
        boolean incorporating_deltas) throws BackoutException
    {
        if ((peered) && (! incorporating_deltas))
            partner_change_log.add(write_key_tuple(key));

        val.get(key).set_val(active_event,to_write.get_val(active_event));
        return;	
    }
		
    public void set_val_on_key(
        ActiveEvent active_event,Integer key, RalphObject<T,D> to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }
	
	
    public void del_key(
        ActiveEvent active_event, Integer key_to_delete,
        boolean incorporating_deltas)
    {
    	/*
          if self.peered and (not incorporating_deltas):
          self.partner_change_log.append(delete_key_tuple(key_to_delete))            
          del self.val[key_to_delete]
        */
    	if (peered && (! incorporating_deltas))
            partner_change_log.add(delete_key_tuple(key_to_delete));
        val.remove(key_to_delete);
    }
    
    public void del_key(ActiveEvent active_event,Integer key_to_delete)
    {
    	del_key(active_event,key_to_delete,false);
    }
    
    public void append(
        ActiveEvent active_event, RalphObject<T,D> new_object,
        boolean incorporating_deltas) throws BackoutException
    {
        Integer key_added = new Integer(val.size());        
    	if (peered && (! incorporating_deltas))
            partner_change_log.add(add_key_tuple(key_added));
    	val.add(key_added,new_object);        
    }
    
    public void append(
        ActiveEvent active_event, RalphObject<T,D> new_object)
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
        RalphObject<T,D> new_val, boolean incorporating_deltas)
        throws BackoutException
    {
        Integer key_added = new Integer(val.size());
        if (peered && (! incorporating_deltas))
            partner_change_log.add(add_key_tuple(key_added));
    	val.add(where_to_insert,new_val);
    }
    
    public void insert(
        ActiveEvent active_event,int where_to_insert,
        RalphObject<T,D> new_val) throws BackoutException
    {
    	insert(active_event,where_to_insert,new_val,false);
    }

    public OpTuple delete_key_tuple(Integer _key)
    {
        return new OpTuple(OpTuple.DELETE_FLAG,_key);
    }
    public boolean is_delete_key_tuple(OpTuple opt)
    {
        return opt.type == OpTuple.DELETE_FLAG;
    }

    public OpTuple add_key_tuple(Integer _key)
    {
        return new OpTuple(OpTuple.ADD_FLAG,_key);
    }
    public boolean is_add_key_tuple(OpTuple opt)
    {
        return opt.type == OpTuple.ADD_FLAG;
    }
	
    public OpTuple write_key_tuple(Integer _key)
    {
        return new OpTuple(OpTuple.WRITE_FLAG,_key);
    }
    public boolean is_write_key(OpTuple opt)
    {
        return opt.type == OpTuple.WRITE_FLAG;
    }	
}
