package ralph;

import ralph.LockedVariables.SingleThreadedLockedMapVariable;
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
}
