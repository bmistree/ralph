package ralph;

import waldo.LockedVariables.SingleThreadedLockedMapVariable;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerActionType;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerAddedKey;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerDeletedKey;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.ContainerAction.ContainerWriteKey;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleInternalListDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleInternalMapDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleListDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SingleMapDelta;
import waldo_protobuffs.VarStoreDeltasProto.VarStoreDeltas.SubElementUpdateActions;

import java.util.ArrayList;
import java.util.HashMap;

import RalphExceptions.BackoutException;


/**
 * 
 * @author bmistree
 *
 * @param <K> --- Keys for the container (Can be Numbers, Booleans, or
 * Strings).
 * @param <V> --- The Java type of data that the keys should point to.
 * @param <D> --- The Java type of data that the internal locked
 * objects should dewaldoify into
 */
public class SingleThreadedLockedContainer<K,V,D> 
    extends SingleThreadedLockedObject <
    // The internal values that these are holding
    HashMap<K,LockedObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    HashMap<K,D>
    >  
    implements ContainerInterface<K,V,D>
{
    private ReferenceTypeDataWrapperConstructor <K,V,D> reference_data_wrapper_constructor =
        null;

    private ReferenceTypeDataWrapper<K,V,D> reference_type_val = null;
	
    public SingleThreadedLockedContainer()
    {
        super();
    }
    public void init(
        String _host_uuid, boolean _peered,
        ReferenceTypeDataWrapperConstructor<K,V,D> rtdwc,
        HashMap<K,LockedObject<V,D>>init_val)
    {
        host_uuid = _host_uuid;
        peered = _peered;
        reference_data_wrapper_constructor = rtdwc;
        reference_type_val =
            (ReferenceTypeDataWrapper<K, V, D>)
            reference_data_wrapper_constructor.construct(init_val, peered); 
        val = reference_type_val;		
    }

    public V get_val_on_key(LockedActiveEvent active_event, K key) 
    {
        /*
         *
         internal_key_val = self.val.val[key]
         if internal_key_val.return_internal_val_from_container():
         return internal_key_val.get_val(active_event)
         return internal_key_val
        */
        LockedObject<V,D> internal_key_val = val.val.get(key);
		
        if (internal_key_val.return_internal_val_from_container())
        {
            Object to_return = null;
            try {
                to_return = internal_key_val.get_val(active_event);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return (V)to_return;
			
        }
		
        return (V) internal_key_val;
    }


    @Override
    public void set_val_on_key(
        LockedActiveEvent active_event, K key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);		
    }
	
	
    @Override
    public void set_val_on_key(
        LockedActiveEvent active_event, K key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        // note: may need to change this to cast to LockedObject<V,D> and use other set_val.
        Util.logger_assert(
            "Should never be setting value directly on container.  " +
            "Instead, should have wrapped V in a LockedObject at an earlier call.");
		
    }	
    public void set_val_on_key(
        LockedActiveEvent active_event, K key, LockedObject<V,D> to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }


	
    public void set_val_on_key(
        LockedActiveEvent active_event, K key, LockedObject<V,D> to_write,
        boolean copy_if_peered) throws BackoutException 
    {
        //def set_val_on_key(self,active_event,key,to_write,copy_if_peered=False):
        //    if copy_if_peered:
        //        if isinstance(to_write,WaldoLockedObj):
        //            to_write = to_write.copy(active_event,True,True)
        //    return self.val.set_val_on_key(active_event,key,to_write)
		
        if (copy_if_peered)
        {
            try {
                to_write = to_write.copy(active_event, true, true);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        reference_type_val.set_val_on_key(active_event,key,to_write);
    }

    
    
    @Override
    public boolean serializable_var_tuple_for_network (
        VarStoreDeltas.Builder parent_delta,String var_name,
        LockedActiveEvent active_event,boolean force)
    {
        CreateInternalMapReturnObj map_return_obj =
            create_internal_map_delta(var_name,active_event,force);
		
        // this contains an extra pruning condition: if we've gotten all the way back to the root context 
        // and haven't observed any change in the subtree, it means that we don't have to send data over 
        // to other side.
        if (map_return_obj.internal_has_been_written ||
            map_return_obj.has_been_written_since_last_msg)
        {
            parent_delta.addMapDeltas(map_return_obj.single_map_delta);
        }

        
        return (force ||
                map_return_obj.internal_has_been_written ||
                map_return_obj.has_been_written_since_last_msg);
    }
    @Override
    public boolean serializable_var_tuple_for_network(
        ContainerWriteKey.Builder parent_delta,
        String var_name, LockedActiveEvent active_event, boolean force) 
    {
        CreateInternalMapReturnObj map_return_obj =
            create_internal_map_delta(var_name,active_event,force);
        parent_delta.setWhatWrittenMap(map_return_obj.single_map_delta);
        return (force ||
                map_return_obj.internal_has_been_written ||
                map_return_obj.has_been_written_since_last_msg);
    }

    @Override
    public boolean serializable_var_tuple_for_network(
        ContainerAddedKey.Builder parent_delta,
        String var_name, LockedActiveEvent active_event, boolean force) 
    {
        CreateInternalMapReturnObj map_return_obj =
            create_internal_map_delta(var_name,active_event,force);
        parent_delta.setAddedWhatMap(map_return_obj.single_map_delta);
        return (force ||
                map_return_obj.internal_has_been_written ||
                map_return_obj.has_been_written_since_last_msg);
    }

    @Override
    public boolean serializable_var_tuple_for_network(
        SubElementUpdateActions.Builder parent_delta,
        String var_name, LockedActiveEvent active_event, boolean force) 
    {
        CreateInternalMapReturnObj map_return_obj =
            create_internal_map_delta(var_name,active_event,force);
        parent_delta.setMapDelta(map_return_obj.single_map_delta);
        return (force ||
                map_return_obj.internal_has_been_written ||
                map_return_obj.has_been_written_since_last_msg);
    }

	
	
    public class CreateInternalMapReturnObj
    {
        public boolean internal_has_been_written;
        public boolean has_been_written_since_last_msg;
        public SingleMapDelta.Builder single_map_delta;
        public CreateInternalMapReturnObj(
            boolean _internal_has_been_written,
            boolean _has_been_written_since_last_msg,
            SingleMapDelta.Builder _single_map_delta)
        {
            internal_has_been_written = _internal_has_been_written;
            has_been_written_since_last_msg = _has_been_written_since_last_msg;
            single_map_delta = _single_map_delta;
        }
		
    }
	
    /**
     * Same parameters as serializable_var_tuple_for_network
     * @return --- null if no subtree has been written and we aren't
     * being forced to create a builder
     */
    private CreateInternalMapReturnObj create_internal_map_delta(
        String var_name, LockedActiveEvent active_event, boolean force)
    {
    	boolean has_been_written_since_last_msg =
            get_and_reset_has_been_written_since_last_msg(active_event);
    	
    	// Just doing maps for now
    	SingleMapDelta.Builder single_map_delta = SingleMapDelta.newBuilder();
    	single_map_delta.setParentType(VarStoreDeltas.ParentType.MAP_CONTAINER);
    	
    	single_map_delta.setVarName(var_name);
    	single_map_delta.setHasBeenWritten(has_been_written_since_last_msg);
    	
    	// check if any sub elements of the map have also been written
    	boolean internal_has_been_written = 
            internal_container_variable_serialize_var_tuple_for_network(
                single_map_delta,var_name,active_event,
                // must force the write when we have written a new value over list
                force || has_been_written_since_last_msg);
    	
    	return new CreateInternalMapReturnObj(
            internal_has_been_written,has_been_written_since_last_msg,
            single_map_delta);
    }
    
    /**
     * 
     * @param single_map_delta
     * @param var_name
     * @param active_event
     * @param force
     * @return --- True if subelement of map has been modified (ie, we
     * need to
     *  serialize and send this branch of the map to the other side).  False otherwise.
     */
    private boolean internal_container_variable_serialize_var_tuple_for_network(
        SingleMapDelta.Builder single_map_delta, String var_name,
        LockedActiveEvent active_event, boolean force) 
    {
		
        // var_data = locked_container.val.val
        HashMap<K,LockedObject<V,D>> var_data = val.val;
		
        // FIXME: If going to have publicly peered data, need to use
        // locked_container.dirty_val instead of locked_container.val when
        // incorporating changes???  .get_dirty_wrapped_val returns
        // wrapped val that can use for serializing data.

        ReferenceTypeDataWrapper <K,V,D> dirty_wrapped_val =
            get_dirty_wrapped_val_reference(active_event);
        boolean sub_element_modified = false;
		
        SingleInternalMapDelta.Builder internal_map_delta =
            SingleInternalMapDelta.newBuilder();
        internal_map_delta.setParentType(
            VarStoreDeltas.ParentType.INTERNAL_MAP_CONTAINER);
        
        if (force)
        {
            // for each item in map, add it to delta as a write action.
            dirty_wrapped_val.add_all_data_to_delta_list(
                internal_map_delta, var_data, active_event, true);
            sub_element_modified = true;
        }
        else
        {
            //# if all subelements have not been modified, then we
            //# do not need to keep track of these changes.
            //# wVariable.waldoMap, wVariable.waldoList, or
            //# wVariable.WaldoUserStruct will get rid of it later.
            sub_element_modified = dirty_wrapped_val.add_to_delta_list(
                internal_map_delta, var_data, active_event, true);
        }
        single_map_delta.setInternalMapDelta(internal_map_delta);
        return sub_element_modified;
    }

	
	
    private ReferenceTypeDataWrapper<K, V, D> get_dirty_wrapped_val_reference(
        LockedActiveEvent active_event)
    {
        return reference_type_val;
    }
    
    @Override
    public HashMap<K, LockedObject<V,D>> get_val(LockedActiveEvent active_event)
    {
    	Util.logger_assert("Cannot call get val on a container object.");
    	return null;
    }
    
    @Override
    public void set_val(
        LockedActiveEvent active_event,
        HashMap<K,LockedObject<V,D>> val_to_set_to)
    {
    	Util.logger_assert("Cannot call set val on a container object directly.");
    }

    @Override
    public void write_if_different(
        LockedActiveEvent active_event,
        HashMap<K, LockedObject<V,D>> new_val)
    {
        // should only call this method on a value type
        Util.logger_assert("Unable to call write if different on container");
    }
  
	

    @Override
    public boolean serializable_var_tuple_for_network(
        SingleListDelta.Builder parent_delta,
        String var_name, LockedActiveEvent active_event, boolean force)
    {
        Util.logger_assert("Should never have parent delta that is a single list in container");
        return false;
    }

    @Override
    public boolean serializable_var_tuple_for_network(
        SingleMapDelta.Builder parent_delta,
        String var_name, LockedActiveEvent active_event, boolean force) 
    {
        Util.logger_assert("Should never have parent delta that is a single map in container");
        return false;
    }


    public DataWrapper<HashMap<K, V>, HashMap<K, D>> get_dirty_wrapped_val(
        LockedActiveEvent active_event)
    {
        Util.logger_assert(
            "Must use dirty_wrapped_val_reference for containers");
        return null;
    }

    @Override
    public int get_len(LockedActiveEvent active_event) 
    {
        return val.val.size();
    }

    @Override
    public ArrayList<K> get_keys(LockedActiveEvent active_event) 
    {
        return new ArrayList<K>(val.val.keySet());
    }

	
	
    @Override
    public void del_key_called(LockedActiveEvent active_event, K key_to_delete) 
    {
        reference_type_val.del_key(active_event, key_to_delete);
    }

    @Override
    public boolean contains_key_called(
        LockedActiveEvent active_event,
        K contains_key) 
    {
        return val.val.containsKey(contains_key);
    }

    @Override
    public boolean contains_val_called(
        LockedActiveEvent active_event,
        V contains_val) 
    {
        return val.val.containsValue(contains_val);
    }

    @Override
    public void incorporate_deltas(
            SingleInternalListDelta delta_to_incorporate,
            LockedActiveEvent active_event)
    {
        // TODO Auto-generated method stub
        Util.logger_assert("Must finish incorporate deltas for list type.");
    }
	
	
    /**
       @param {WaldoLockedContainer or SingleThreadedLockedContainer}
	
       @param {SingleListDelta or SingleMapDelta} delta_to_incorporate
	
       @param {SingleInternalListDelta or SingleInternalMapDelta}
       delta_to_incorporate
	
       When a peered or sequence peered container (ie, map, list, or
       struct) is modified by one endpoint, those changes must be
       reflected on the other endpoint.  This method takes the
       changes that one endpoint has made on a container, represented
       by delta_to_incorporate, and applies them (if we can).    
    */
    @Override
    public void incorporate_deltas(
        SingleInternalMapDelta delta_to_incorporate,
        LockedActiveEvent active_event) 
    {
        for (ContainerAction action: delta_to_incorporate.getMapActionsList())
        {
            if (action.getContainerAction() == ContainerActionType.WRITE_VALUE)
            {
                // data written to map
                ContainerWriteKey container_written_action = action.getWriteKey();
                K index_to_write_to =
                    get_write_key_incorporate_deltas(container_written_action);
				
                V new_val = null;
                if (container_written_action.hasWhatWrittenText())
                {
                    new_val = (V) (container_written_action.getWhatWrittenText());
                }
                else if (container_written_action.hasWhatWrittenNum())
                    new_val = (V) (new Double(container_written_action.getWhatWrittenNum()));
                else if (container_written_action.hasWhatWrittenTf())
                    new_val = (V) (new Boolean(container_written_action.getWhatWrittenTf()));
                else if (container_written_action.hasWhatWrittenMap())
                {
                    SingleThreadedLockedMapVariable new_sub_map =
                        new LockedVariables.SingleThreadedLockedMapVariable(host_uuid,true);
                    new_val = (V) new_sub_map;
                    SingleMapDelta sub_map_delta = container_written_action.getWhatWrittenMap();
                    new_sub_map.incorporate_deltas(sub_map_delta, active_event);
                }
                else
                {
                    Util.logger_assert(
                        "Not handling deserializing any nested containers " +
                        "yet beyond nested maps.");
                }
				
                // actually update the value on the target key
                // single threaded can't back out
                try {
                    set_val_on_key(active_event, index_to_write_to, new_val);
                } catch (BackoutException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
				
            }// closes if WRITE_VALUE
            else if (action.getContainerAction() == ContainerActionType.ADD_KEY)
            {
                // data added to map
                ContainerAddedKey container_added_action = action.getAddedKey();
                K index_to_add_to = get_add_key_incorporate_deltas(container_added_action);
				
                V new_val = null;
                if (container_added_action.hasAddedWhatText())
                {
                    new_val = (V) (container_added_action.getAddedWhatText());
                }
                else if (container_added_action.hasAddedWhatNum())
                    new_val = (V) (new Double(container_added_action.getAddedWhatNum()));
                else if (container_added_action.hasAddedWhatTf())
                    new_val = (V) (new Boolean(container_added_action.getAddedWhatTf()));
                else if (container_added_action.hasAddedWhatMap())
                {
                    SingleThreadedLockedMapVariable new_sub_map =
                        new LockedVariables.SingleThreadedLockedMapVariable(host_uuid,true);
                    new_val = (V) new_sub_map;
                    SingleMapDelta sub_map_delta = container_added_action.getAddedWhatMap();
                    new_sub_map.incorporate_deltas(sub_map_delta, active_event);
                }
                else
                {
                    Util.logger_assert(
                        "Not handling deserializing any nested containers " +
                        "yet beyond nested maps.");
                }
				
                // actually add the target key
                handle_added_key_incorporate_deltas(
                    active_event,index_to_add_to,new_val);
				
            }// closes if ADD_KEY
            else if (action.getContainerAction() == ContainerActionType.DELETE_KEY)
            {
                // data deleted from map
                ContainerDeletedKey container_deleted_action =
                    action.getDeletedKey();
                K index_to_delete_from =
                    get_delete_key_incorporate_deltas(container_deleted_action);
                del_key_called(active_event,index_to_delete_from);

            }
            else
            {
                Util.logger_assert("Unknown action type when deserializing map.");
            }
        }
    }
	
	
	
    private void handle_added_key_incorporate_deltas(
        LockedActiveEvent active_event, K index_to_add_to, V new_val)
    {
        // for map, just call set_val_on_key...will take care of inserting a 
        // new entry in map for me.
		
        // single threaded cannot back out
        try {
            this.set_val_on_key(active_event, index_to_add_to, new_val);
        } catch (BackoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Create separate methods to get appropriate keys out of container actions
     * @param container_deleted_action
     * @return
     */
    private K get_delete_key_incorporate_deltas(
        ContainerDeletedKey container_deleted_action) 
    {
        if (container_deleted_action.hasDeletedKeyText())
            return (K) container_deleted_action.getDeletedKeyText();
        else if (container_deleted_action.hasDeletedKeyNum())
            return (K) (new Double(container_deleted_action.getDeletedKeyNum()));
        else if (container_deleted_action.hasDeletedKeyTf())
            return (K) (new Boolean(container_deleted_action.getDeletedKeyTf()));
		
        Util.logger_assert("Error: unknown key type for map in delete key");
        return null;	
    }
	

    private K get_add_key_incorporate_deltas(
        ContainerAddedKey container_added_action) 
    {
        if (container_added_action.hasAddedKeyText())
            return (K) container_added_action.getAddedKeyText();
        else if (container_added_action.hasAddedKeyNum())
            return (K) (new Double(container_added_action.getAddedKeyNum()));
        else if (container_added_action.hasAddedKeyTf())
            return (K) (new Boolean(container_added_action.getAddedKeyTf()));
		
        Util.logger_assert("Error: unknown key type for map in delete key");
        return null;	
    }

    private K get_write_key_incorporate_deltas(ContainerWriteKey container_written_action)
    {
        if (container_written_action.hasWriteKeyText())
            return (K) container_written_action.getWriteKeyText();
        else if (container_written_action.hasWriteKeyNum())
            return (K) (new Double(container_written_action.getWriteKeyNum()));
        else if (container_written_action.hasWriteKeyTf())
            return (K) (new Boolean(container_written_action.getWriteKeyTf()));
		
        Util.logger_assert("Error: unknown key type for map in write key");
        return null;
    }
		
}
