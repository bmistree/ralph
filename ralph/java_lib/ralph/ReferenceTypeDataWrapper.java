package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import RalphExceptions.BackoutException;


import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerAddedKey;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerDeletedKey;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerWriteKey;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ParentType;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleInternalMapDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SubElementUpdateActions;

/**
 * 
 * @author bmistree
 *
 * @param <K> --- The key type
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 * @param <D> --- What the java variables in the hashmap should
 * dewaldoify into (if they are locked objects)
 */
public class ReferenceTypeDataWrapper<K,T,D>
    extends DataWrapper<HashMap<K,LockedObject<T,D>>, HashMap<K,D>>{
	
    class OpTuple
    {
        public static final int DELETE_FLAG = 0;
        public static final int ADD_FLAG = 1;
        public static final int WRITE_FLAG = 2;
		
        public int type;
        public K key;
        public OpTuple(int _type, K _key)
        {
            type = _type;
            key = _key;
        }		
		
    }
	
    private boolean peered;
    //protected HashMap<K,LockedObject<T>> val;
    protected boolean has_been_written_since_last_msg = false;
	
	
    /*
     * tracks all insertions, removals, etc. made to this reference
     * object so can send deltas across network to partners.
     * (Note: only used for peered data.)
     */
    private ArrayList <OpTuple> partner_change_log = new ArrayList<OpTuple>(); 

	
    public ReferenceTypeDataWrapper(HashMap<K,LockedObject<T,D>> v, boolean _peered)
    {
        super( (HashMap<K,LockedObject<T,D>>)v.clone(),_peered);
        peered = _peered;
        //val = (HashMap<K,LockedObject<T>>)v.clone();		
    }
	
	
    public ReferenceTypeDataWrapper(ReferenceTypeDataWrapper<K,T,D> v, boolean _peered)
    {
        super(v.val,_peered);
        peered = _peered;
    }
	
    public HashMap<K,D> de_waldoify(LockedActiveEvent active_event) throws BackoutException
    {
        HashMap<K,D>to_return_map = new HashMap<K,D>();			
        for (Map.Entry<K, LockedObject<T,D>> entry : val.entrySet())
        {
            LockedObject<T,D> locked_obj = entry.getValue();
            to_return_map.put(entry.getKey(), locked_obj.de_waldoify(active_event) );
        }		
        return to_return_map;        
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
        LockedActiveEvent active_event,K key, LockedObject<T,D> to_write,
        boolean incorporating_deltas) throws BackoutException
    {
        if (! val.containsKey(key))
        {
            add_key(active_event,key,to_write,incorporating_deltas);
            return;
        }
			
        if ((peered) && (! incorporating_deltas))
            partner_change_log.add(write_key_tuple(key));

		
        //if isinstance(to_write,WaldoLockedObj):
        //    return self.val[key].set_val(active_event,to_write.get_val(active_event))
		
        val.get(key).set_val(active_event,to_write.get_val(active_event));
        return;	
    }
		
    public void set_val_on_key(
        LockedActiveEvent active_event,K key, LockedObject<T,D> to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }
	
	

    public void del_key(
        LockedActiveEvent active_event, K key_to_delete,
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
    
    public void del_key(LockedActiveEvent active_event,K key_to_delete)
    {
    	del_key(active_event,key_to_delete,false);
    }
    

    public void add_key(
        LockedActiveEvent active_event, K key_added,
        LockedObject<T,D> new_object, boolean incorporating_deltas)
    {
    	/*
          if self.peered and (not incorporating_deltas):
          self.partner_change_log.append(add_key_tuple(key_added))

          self.val[key_added] = new_val
        */
    	
    	if (peered && (! incorporating_deltas))
            partner_change_log.add(add_key_tuple(key_added));
    	
    	val.put(key_added,new_object);    	
    }
    
    public void add_key(
        LockedActiveEvent active_event,K key_to_add,LockedObject<T,D> new_object)
    {
    	add_key(active_event,key_to_add,new_object,false);
    }

    
    /**
     * Can only be called on list, so where to insert is int
     * @param active_event
     * @param where_to_insert
     * @param new_val
     * @param incorporating_deltas
     */
    public void insert(
        LockedActiveEvent active_event, int where_to_insert,
        LockedObject<T,D> new_val, boolean incorporating_deltas)
    {
    	Util.logger_assert("Cannot insert on map");
    }

    public void insert(
        LockedActiveEvent active_event,int where_to_insert,
        LockedObject<T,D> new_val)
    {
    	insert(active_event,where_to_insert,new_val,false);
    }

    /**
     * Can only be called on list.
     * @param active_event
     * @param new_val
     * @param incorporating_deltas
     */
    public void append(
        LockedActiveEvent active_event, LockedObject<T,D> new_val,
        boolean incorporating_deltas)
    {
    	Util.logger_assert("Can only append to list");
    }
    
    public void append(
        LockedActiveEvent active_event, LockedObject<T,D> new_val)
    {
    	append(active_event,new_val,false);
    }

    
    
    /**
     * 
     Run through entire list.  Create an add action for each element.

     @param {bool} for_map --- True if performing operations for
     map.  false if performing for list.
       
     * @param delta_to_add_to
     * @param current_internal_val
     * @param active_event
     * @param for_map
     */
    public void add_all_data_to_delta_list(
        SingleInternalMapDelta.Builder delta_to_add_to,
        HashMap<K,LockedObject<T,D>> current_internal_val,
        LockedActiveEvent active_event,boolean for_map)
    {
    	if (!for_map)
            Util.logger_assert("Using incorrect ReferenceTypeDataWrapper");
    	
    	for (Map.Entry<K, LockedObject<T,D>> entry : val.entrySet())
    	{
            K key = entry.getKey();    		
            //action = delta_to_add_to.map_actions.add()
            ContainerAction.Builder action = ContainerAction.newBuilder();
            //action.container_action = VarStoreDeltas.ContainerAction.ADD_KEY
            action.setContainerAction(VarStoreDeltas.ContainerAction.ContainerActionType.ADD_KEY);
    		
            // add_action = action.added_key
            ContainerAddedKey.Builder add_action = ContainerAddedKey.newBuilder();
            // add_action.parent_type = VarStoreDeltas.CONTAINER_ADDED
            add_action.setParentType(VarStoreDeltas.ParentType.CONTAINER_ADDED);

    		
            if (Number.class.isInstance(key))
            {
                Number n = (Number)key;
                add_action.setAddedKeyNum(n.doubleValue());
            }
            else if (String.class.isInstance(key))
            {
                String k = (String)key;
                add_action.setAddedKeyText(k);    			
            }
            else
            {
                Boolean k = (Boolean)key;
                add_action.setAddedKeyTf(k);
            }

            /*
              # now actually add the value to the map
              list_val = current_internal_val[key]
            */
            Object internal_val = current_internal_val.get(key);
            if (Number.class.isInstance(internal_val))
            {
                Number casted_internal_val = (Number)internal_val;
                add_action.setAddedWhatNum(casted_internal_val.doubleValue());
            }
            else if (String.class.isInstance(internal_val))
            {
                String casted_internal_val = (String)internal_val;
                add_action.setAddedWhatText(casted_internal_val);    			
            }
            else if (Boolean.class.isInstance(internal_val))
            {
                // must be bool
                Boolean casted_internal_val = (Boolean)internal_val;
                add_action.setAddedWhatTf(casted_internal_val);    			
            }
            else if (LockedObject.class.isInstance(internal_val))
            {
                LockedObject casted_internal_val = (LockedObject)internal_val;
                casted_internal_val.serializable_var_tuple_for_network(add_action,"",active_event,true);
            }
            else
                Util.logger_assert("Unknown intneral val");
    		
            action.setAddedKey(add_action);
            delta_to_add_to.addMapActions(action);
    	}
    	
    }
    
    /**
     *
     @param {SingleInternalMapDelta.Builder} delta_to_add_to ---

     @param {list} current_internal_val --- The internal val of the action event.

     @param {_InvalidationListener} action_event

     @param {bool} for_map --- True if performing operations for
     map.  false if performing for list.
            
     @returns {bool} --- Returns true if have any changes to add false otherwise.
            
     * 
     * @return
     */
    public boolean add_to_delta_list(
        SingleInternalMapDelta.Builder delta_to_add_to,
        HashMap<K,LockedObject<T,D>> current_internal_val,
        LockedActiveEvent active_event,boolean for_map)
    {
    	HashMap<K,Boolean> modified_indices = new HashMap<K,Boolean>();
    	boolean changes_made = false;
    	
    	for (OpTuple partner_change : partner_change_log)
    	{
            changes_made = true;
            //action = delta_to_add_to.map_actions.add()
            ContainerAction.Builder action = ContainerAction.newBuilder();
    		
            if (is_delete_key_tuple(partner_change))
            {
                /*
                 * action.container_action = VarStoreDeltas.ContainerAction.DELETE_KEY
                 delete_action = action.deleted_key
                 key = partner_change[1]
                 modified_indices[key] = True
                 if isinstance(key,numbers.Number):
                 delete_action.deleted_key_num = key
                 elif util.is_string(key):
                 delete_action.deleted_key_text = key
                 elif isinstance(key,bool):
                 delete_action.deleted_key_tf = key
                 * 
                 */
                action.setContainerAction(VarStoreDeltas.ContainerAction.ContainerActionType.DELETE_KEY);
                ContainerDeletedKey.Builder delete_action = ContainerDeletedKey.newBuilder();
    			
                K key = partner_change.key;
                modified_indices.put(key, true);
    			
                if (Number.class.isInstance(key))
                {
                    Number n = (Number)key;
                    delete_action.setDeletedKeyNum(n.doubleValue());
                }
                else if (String.class.isInstance(key))
                    delete_action.setDeletedKeyText((String)key);
                else if (Boolean.class.isInstance(key))
                    delete_action.setDeletedKeyTf((Boolean)key);
                else
                    Util.logger_assert("Unknown key type when serializing");
    			
                action.setDeletedKey(delete_action);
            }
            else if (is_add_key_tuple(partner_change))
            {
                K key = partner_change.key;
                modified_indices.put(key, true);
                boolean key_in_internal = current_internal_val.containsKey(key);
    				
                if (key_in_internal)
                {
                    /*
                      # note, key may not be in internal val, for
                      # instance if we had deleted it after adding.
                      # in this case, can ignore the add here.
                    */
    				
                    //action.container_action = VarStoreDeltas.ContainerAction.ADD_KEY
                    action.setContainerAction(VarStoreDeltas.ContainerAction.ContainerActionType.ADD_KEY);
        		
                    // add_action = action.added_key
                    ContainerAddedKey.Builder add_action = ContainerAddedKey.newBuilder();
    				
                    //add_action.parent_type = VarStoreDeltas.CONTAINER_ADDED
                    add_action.setParentType(VarStoreDeltas.ParentType.CONTAINER_ADDED);

                    /*
                     *  if isinstance(key,numbers.Number):
                     add_action.added_key_num = key
                     elif util.is_string(key):
                     add_action.added_key_text = key
                     elif isinstance(key,bool):
                     add_action.added_key_tf = key
                     #### DEBUG
                     else:
                     util.logger_assert('Unknown map key type when serializing')
                     #### END DEBUG

                    */
                    if (Number.class.isInstance(key))
                        add_action.setAddedKeyNum(((Number)key).doubleValue());
                    else if (String.class.isInstance(key))
                        add_action.setAddedKeyText((String)key);    			
                    else
                        add_action.setAddedKeyTf((Boolean)key);


    	            //# now actually add the value to the map
    	            Object internal_val = current_internal_val.get(key);
    	            if (Number.class.isInstance(internal_val))
    	            	add_action.setAddedWhatNum(((Double)internal_val).doubleValue());
    	            else if (String.class.isInstance(internal_val))
    	            	add_action.setAddedWhatText(((String)internal_val));
    	            else if (Boolean.class.isInstance(internal_val))
    	            	add_action.setAddedWhatTf((Boolean)internal_val);
    	            else
    	            {
    	            	LockedObject waldo_var = (LockedObject)internal_val;
    	            	waldo_var.serializable_var_tuple_for_network(
                            add_action,"",active_event,
                            // true here because if anything is written
                            // or added, then we must force the entire
                            // copy of it.
                            true);
    	            }
    	            
    	            action.setAddedKey(add_action);
    				
                } // ends if key_in_internal	
            }
            else if (is_write_key(partner_change))
            {
                K key = partner_change.key;
                modified_indices.put(key, true);
                boolean key_in_internal = current_internal_val.containsKey(key);
    			
                if (key_in_internal)
                {
                    //action.container_action = VarStoreDeltas.ContainerAction.WRITE_VALUE
                    action.setContainerAction(VarStoreDeltas.ContainerAction.ContainerActionType.WRITE_VALUE);
                    //write_action = action.write_key
                    ContainerWriteKey.Builder write_action = ContainerWriteKey.newBuilder();
        			
        			
                    //write_action.parent_type = VarStoreDeltas.CONTAINER_WRITTEN
                    write_action.setParentType(VarStoreDeltas.ParentType.CONTAINER_WRITTEN);
        			
                    /*
                     *  if isinstance(key,numbers.Number):
                     write_action.write_key_num = key
                     elif util.is_string(key):
                     write_action.write_key_text = key
                     elif isinstance(key,bool):
                     write_action.write_key_tf = key
                     #### DEBUG
                     else:
                     util.logger_assert('Unknown map key type when serializing')
                     #### END DEBUG

                    */
                    if (Number.class.isInstance(key))
                        write_action.setWriteKeyNum(((Number)key).doubleValue());
                    else if (String.class.isInstance(key))
                        write_action.setWriteKeyText((String)key);
                    else if (Boolean.class.isInstance(key))
                        write_action.setWriteKeyTf((Boolean)key);
                    else
                        Util.logger_assert("Unknown map type when serializing");

                    Object internal_val = current_internal_val.get(key);
                    if (Number.class.isInstance(internal_val))
                        write_action.setWhatWrittenNum(((Number)internal_val).doubleValue());
                    else if (String.class.isInstance(internal_val))
                    	write_action.setWhatWrittenText((String)internal_val);
                    else if (Boolean.class.isInstance(internal_val))
                    	write_action.setWhatWrittenTf((Boolean)internal_val);
                    else
                    {
                    	LockedObject casted_internal_val = (LockedObject)internal_val;
                    	casted_internal_val.serializable_var_tuple_for_network(
                            write_action,"",active_event,
                            /*
                              # true here because if anything is written
                              # or added, then we must force the entire
                              # copy of it.
                            */
                            true);
                    }
                    action.setWriteKey(write_action);
                }// ends if key_in_internal
            } // ends else if
            delta_to_add_to.addMapActions(action);
    		
    	} // ends for partner change loop
    	
    	
    	for (Map.Entry<K, LockedObject<T,D>> entry :
                 current_internal_val.entrySet())
    	{
            LockedObject<T,D> casted_internal_val = entry.getValue();
            K index = entry.getKey();
            if (!modified_indices.containsKey(index))
            {
                // create action
                // sub_element_action = delta_to_add_to.sub_element_update_actions.add()     
                SubElementUpdateActions.Builder sub_element_action =
                    SubElementUpdateActions.newBuilder();
    			
    			
                //sub_element_action.parent_type = VarStoreDeltas.SUB_ELEMENT_ACTION
                sub_element_action.setParentType(
                    VarStoreDeltas.ParentType.SUB_ELEMENT_ACTION);
    			
                if (Number.class.isInstance(index))
                    sub_element_action.setKeyNum(((Number)index).doubleValue());
                else if (String.class.isInstance(index))
                    sub_element_action.setKeyText((String)index);
                else
                    sub_element_action.setKeyTf((Boolean)index);

                if (casted_internal_val.serializable_var_tuple_for_network(
                        sub_element_action,"",active_event,false))
                {
                    changes_made = true;
                }
                else
                {
                    // no change made to subtree: go ahead and delete added subaction
                    // remove last subaction from subelement update actions
                    int size_sub_element_actions = delta_to_add_to.getSubElementUpdateActionsCount();
                    delta_to_add_to.removeSubElementUpdateActions(size_sub_element_actions -1);
                }
                delta_to_add_to.addSubElementUpdateActions(sub_element_action);
            }
    	} //closes for loop over current_internal_val
    	/*
          # clean out change log: do not need to re-send updates for
          # these changes to partner, so can just reset after sending
          # once.
        */
        partner_change_log = new ArrayList<OpTuple>();
        return changes_made;
    }
	

    public OpTuple delete_key_tuple(K _key)
    {
        return new OpTuple(OpTuple.DELETE_FLAG,_key);
    }
    public boolean is_delete_key_tuple(OpTuple opt)
    {
        return opt.type == OpTuple.DELETE_FLAG;
    }

    public OpTuple add_key_tuple(K _key)
    {
        return new OpTuple(OpTuple.ADD_FLAG,_key);
    }
    public boolean is_add_key_tuple(OpTuple opt)
    {
        return opt.type == OpTuple.ADD_FLAG;
    }
	
    public OpTuple write_key_tuple(K _key)
    {
        return new OpTuple(OpTuple.WRITE_FLAG,_key);
    }
    public boolean is_write_key(OpTuple opt)
    {
        return opt.type == OpTuple.WRITE_FLAG;
    }	
}
