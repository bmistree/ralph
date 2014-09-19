package RalphDataWrappers;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import ralph.RalphObject;
import ralph.ActiveEvent;
import ralph.Util;
import ralph.VersioningInfo;

import RalphExceptions.BackoutException;

/**
 * @param <KeyType> --- The key type
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 */
public class MapTypeDataWrapper<KeyType,ValueType,DeltaValueType>
    extends DataWrapper<Map<KeyType,RalphObject<ValueType,DeltaValueType>>>
{
    private final boolean log_changes;

    private final Class<KeyType> key_type_class;
    private final Class<ValueType> value_type_class;
    
    /*
     * tracks all insertions, removals, etc. made to this reference
     * object so can send deltas across network to partners.
     * (Note: only used for log_changes data.)
     */
    private final List <ContainerOpTuple<KeyType,ValueType,DeltaValueType>>
        change_log;

    public MapTypeDataWrapper(
        Map<KeyType,RalphObject<ValueType,DeltaValueType>> v,
        Class<KeyType> _key_type_class,Class<ValueType> _value_type_class,
        boolean _log_changes)
    {
        super(new HashMap<KeyType,RalphObject<ValueType,DeltaValueType>>(v));

        key_type_class = _key_type_class;
        value_type_class = _value_type_class;

        // either perform logging if global logging switch is on, or
        // if explicitly told to.
        log_changes =
            (VersioningInfo.instance.local_version_saver != null) ||
            _log_changes;
        
        if (log_changes)
        {
            change_log =
                new ArrayList<ContainerOpTuple<KeyType,ValueType,DeltaValueType>>();
        }
        else
            change_log = null;

    }

    public MapTypeDataWrapper(
        MapTypeDataWrapper<KeyType,ValueType,DeltaValueType> v,
        Class<KeyType> _key_type_class,Class<ValueType> _value_type_class,
        boolean _log_changes)
    {
        this(v.val,_key_type_class,_value_type_class,_log_changes);
    }

    /**
       Reference is unmodifiable.
     */
    public List<ContainerOpTuple<KeyType,ValueType,DeltaValueType>>
        get_unmodifiable_change_log()
    {
        return Collections.unmodifiableList(change_log);
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
     * To ensure that we aren't copying references to value variables,
     * we actually call get_val on to_write and use that value.
     * 
     */
    public void set_val_on_key(
        ActiveEvent active_event,KeyType key,
        RalphObject<ValueType,DeltaValueType> to_write,
        boolean incorporating_deltas) throws BackoutException
    {
        if (! val.containsKey(key))
        {
            add_key(active_event,key,to_write,incorporating_deltas);
            return;
        }
			
        if ((log_changes) && (! incorporating_deltas))
            change_log.add(write_key_tuple(key,to_write));

        val.get(key).set_val(active_event,to_write.get_val(active_event));
        return;	
    }
		
    public void set_val_on_key(
        ActiveEvent active_event,KeyType key,
        RalphObject<ValueType,DeltaValueType> to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }

    public void del_key(
        ActiveEvent active_event, KeyType key_to_delete,
        boolean incorporating_deltas)
    {
    	if (log_changes && (! incorporating_deltas))
            change_log.add(delete_key_tuple(key_to_delete));
        val.remove(key_to_delete);
    }
    
    public void del_key(
        ActiveEvent active_event,KeyType key_to_delete)
    {
    	del_key(active_event,key_to_delete,false);
    }
    
    public void add_key(
        ActiveEvent active_event, KeyType key_added,
        RalphObject<ValueType,DeltaValueType> new_object,
        boolean incorporating_deltas)
    {
    	if (log_changes && (! incorporating_deltas))
            change_log.add(add_key_tuple(key_added,new_object));
    	
    	val.put(key_added,new_object);    	
    }
    
    public void add_key(
        ActiveEvent active_event,KeyType key_to_add,
        RalphObject<ValueType,DeltaValueType> new_object)
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
        ActiveEvent active_event, int where_to_insert,
        RalphObject<ValueType,DeltaValueType> new_val,
        boolean incorporating_deltas)
    {
    	Util.logger_assert("Cannot insert on map");
    }

    public void insert(
        ActiveEvent active_event,int where_to_insert,
        RalphObject<ValueType,DeltaValueType> new_val)
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
        ActiveEvent active_event, RalphObject<ValueType,DeltaValueType> new_val,
        boolean incorporating_deltas)
    {
    	Util.logger_assert("Can only append to list");
    }
    
    public void append(
        ActiveEvent active_event, RalphObject<ValueType,DeltaValueType> new_val)
    {
    	append(active_event,new_val,false);
    }

    public ContainerOpTuple<KeyType,ValueType,DeltaValueType> delete_key_tuple(KeyType _key)
    {
        return
            new ContainerOpTuple<KeyType,ValueType,DeltaValueType>(
                ContainerOpTuple.OpType.DELETE,_key,null);
    }
    public ContainerOpTuple <KeyType,ValueType,DeltaValueType> add_key_tuple(
        KeyType _key,RalphObject<ValueType,DeltaValueType> what_added)
    {
        return new ContainerOpTuple<KeyType,ValueType,DeltaValueType>(
            ContainerOpTuple.OpType.ADD,_key,what_added);
    }
    public ContainerOpTuple<KeyType,ValueType,DeltaValueType> write_key_tuple(
        KeyType _key,RalphObject<ValueType,DeltaValueType> what_written)
    {
        return new <KeyType,ValueType,DeltaValueType>ContainerOpTuple(
            ContainerOpTuple.OpType.WRITE,_key,what_written);
    }
}
