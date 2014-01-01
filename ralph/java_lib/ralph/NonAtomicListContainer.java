package ralph;

import java.util.ArrayList;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;

/**
 * @param <V> --- The Java type of data that the values should point to.
 * @param <D> --- The Java type of data that the internal locked
 * objects should dewaldoify into
 */
public class NonAtomicListContainer<V,D> 
    extends NonAtomicObject <
    // The internal values that these are holding
    ArrayList<RalphObject<V,D>>,
    // When call dewaldoify on this container, what we should get back
    ArrayList<D>
    >  
    implements ContainerInterface<Integer,V,D>
{
    public EnsureAtomicWrapper<V,D>locked_wrapper;
    
    private ListTypeDataWrapperFactory <V,D> reference_data_wrapper_constructor =
        null;

    private ListTypeDataWrapper<V,D> reference_type_val = null;
	
    public NonAtomicListContainer()
    {
        super();
    }
    public void init(
        String _host_uuid, boolean _peered,
        ListTypeDataWrapperFactory<V,D> rtdwc,
        ArrayList<RalphObject<V,D>>init_val,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        host_uuid = _host_uuid;
        peered = _peered;
        locked_wrapper = _locked_wrapper;
        reference_data_wrapper_constructor = rtdwc;
        reference_type_val =
            (ListTypeDataWrapper<V, D>)
            reference_data_wrapper_constructor.construct(init_val, peered); 
        val = reference_type_val;		
    }

    public V get_val_on_key(
        ActiveEvent active_event, Double key)  throws BackoutException
    {
        return get_val_on_key(
            active_event,new Integer((int)key.doubleValue()));
    }

    
    @Override
    public V get_val_on_key(ActiveEvent active_event, Integer key)
        throws BackoutException
    {
        /*
         *
         internal_key_val = self.val.val[key]
         if internal_key_val.return_internal_val_from_container():
         return internal_key_val.get_val(active_event)
         return internal_key_val
        */
        RalphObject<V,D> internal_key_val = val.val.get(key);
		
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

    /**
       Runs through all the entries in the map/list/struct and puts
       them into any_builder.
     */
    public void serialize_as_rpc_arg (
        ActiveEvent active_event, VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        // FIXME: must allow serializing lists as rpcs
        Util.logger_assert(
            "FIXME: must allow serializing lists for rpc args.");
    }
    

    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);		
    }
	
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        // note: may need to change this to cast to RalphObject<V,D> and use other set_val.
        Util.logger_assert(
            "Should never be setting value directly on container.  " +
            "Instead, should have wrapped V in a RalphObject at an earlier call.");
		
    }	
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,D> to_write)
        throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);
    }

    public void set_val_on_key(
        ActiveEvent active_event, Double key, RalphObject<V,D> to_write)
        throws BackoutException
    {
        set_val_on_key(
            active_event,new Integer((int)key.doubleValue()),to_write,false);
    }
    
    public void set_val_on_key(
        ActiveEvent active_event, Double key, V to_write)
        throws BackoutException
    {
        RalphObject<V,D> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(to_write);
        set_val_on_key(
            active_event,new Integer((int)key.doubleValue()),wrapped_to_write);
    }
    
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, RalphObject<V,D> to_write,
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
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        Util.logger_assert(
            "Still must define swap method for NonAtomicListContainer.");
    }
    
    @Override
    public ArrayList<RalphObject<V,D>> get_val(ActiveEvent active_event)
    {
    	Util.logger_assert("Cannot call get val on a container object.");
    	return null;
    }
    
    @Override
    public void set_val(
        ActiveEvent active_event,
        ArrayList<RalphObject<V,D>> val_to_set_to)
    {
    	Util.logger_assert("Cannot call set val on a container object directly.");
    }

    public void append(
        ActiveEvent active_event, V what_to_insert)
        throws BackoutException
    {
        RalphObject<V,D> wrapped_to_insert =
            locked_wrapper.ensure_atomic_object(what_to_insert);
        int size = val.val.size();
        Integer index_to_insert_in = new Integer(size);
        insert(
            active_event, index_to_insert_in, wrapped_to_insert);
    }

    
    @Override
    public void write_if_different(
        ActiveEvent active_event,
        ArrayList<RalphObject<V,D>> new_val)
    {
        // should only call this method on a value type
        Util.logger_assert("Unable to call write if different on container");
    }

    @Override
    public int get_len(ActiveEvent active_event) throws BackoutException
    {
        return val.val.size();
    }

    @Override
    public Double get_len_boxed(ActiveEvent active_event) 
        throws BackoutException
    {
        return new Double(get_len(active_event));
    }

    @Override
    public ArrayList<Integer> get_keys(ActiveEvent active_event)
        throws BackoutException
    {
        Util.logger_assert(
            "FIXME: should be returning RalphObject of lists.");
        return null;
    }

    @Override
    public void del_key_called(ActiveEvent active_event, Integer key_to_delete) throws BackoutException
    {
        reference_type_val.del_key(active_event, key_to_delete);
    }

    @Override
    public boolean contains_key_called(
        ActiveEvent active_event,
        Integer contains_key) throws BackoutException
    {
        int internal_val = contains_key.intValue();
        int list_size = val.val.size();
        boolean to_return = internal_val < list_size;
        return to_return;
    }

    @Override
    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, Integer contains_key) throws BackoutException
    {
        return new Boolean(
            contains_key_called(active_event,contains_key));
    }    

    public Boolean contains_key_called_boxed(
        ActiveEvent active_event, Double contains_key) throws BackoutException
    {
        return new Boolean(
            contains_key_called(
                active_event,
                new Integer((int)contains_key.doubleValue())));
    }    

    
    @Override
    public boolean contains_val_called(
        ActiveEvent active_event,
        V contains_val) throws BackoutException
    {
        return val.val.contains(contains_val);
    }

    @Override
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in, V what_to_insert)
        throws BackoutException
    {
        Util.logger_assert(
            "Need to override list inertion code in parent.");
    }
    @Override
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in,
        RalphObject<V,D> what_to_insert)
        throws BackoutException
    {
        val.val.add(index_to_insert_in,what_to_insert);
    }
}
