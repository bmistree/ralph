package ralph;

import java.util.ArrayList;
import java.util.Map.Entry;
import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;



/**
 * @param <V>  ---- The type of each internal value in the internal arraylist
 * @param <D>  ---- The type that each value in the internal arraylist
 * 
 * A list of strings:
 * 
 * LockedListVariable<String,String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number, ArrayList<Number > >
 *     ArrayList<Number,ArrayList<Number>>>
 * 
 */
public abstract class AtomicList<V,D>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return AtomicListContainer.
    // when call set val, must pass in a AtomicListContainer
    AtomicInternalList<V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public AtomicList(
        String _host_uuid, boolean _peered,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super();
        
        AtomicInternalList<V,D> init_val = new AtomicInternalList<V,D>();
        init_val.init_multithreaded_list_container(
            _host_uuid, _peered,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        AtomicInternalList<V,D> default_val = new AtomicInternalList<V,D>();
        default_val.init_multithreaded_list_container(
            _host_uuid, _peered,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _host_uuid, _peered, init_val,default_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,D>,D>());
    }
    
    
    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedList.
       This constructor is for this.
     */
    public AtomicList(
        String _host_uuid, boolean _peered,
        AtomicInternalList<V,D> internal_val,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super();
        
        AtomicInternalList<V,D> default_val = new AtomicInternalList<V,D>();
        default_val.init_multithreaded_list_container(
            _host_uuid, _peered,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _host_uuid, _peered, internal_val,default_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,D>,D>());
    }

    
    public AtomicList(
        String _host_uuid, boolean _peered,
        ArrayList<RalphObject<V,D>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super();

        AtomicInternalList<V,D> init_val_2 = new AtomicInternalList<V,D>();
        init_val_2.init_multithreaded_list_container(
            _host_uuid, _peered,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        AtomicInternalList<V,D> default_val = new AtomicInternalList<V,D>();
        default_val.init_multithreaded_list_container(
            _host_uuid, _peered,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _host_uuid, _peered, init_val_2,default_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,D>,D>());

        load_init_vals(init_val,incorporating_deltas);
    }
    

    
    public D de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        Util.logger_warn("Must acquire read lock when de waldoifying.");
        return val.de_waldoify(active_event);
    }

    public boolean return_internal_val_from_container()
    {
        return false;
    }


    public void serialize_as_rpc_arg(
        ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        Util.logger_assert("FIXME: finish serializing lists.");
        // AtomicMapContainer<K,V,D> internal_val =
        //     get_val(active_event);
        // internal_val.serialize_as_rpc_arg(
        //     active_event,any_builder,is_reference);
    }

    public void load_init_vals(
        ArrayList<RalphObject<V,D>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<V,D> to_load: init_val)
        {
            ListTypeDataWrapper<V,D>casted_wrapper = (ListTypeDataWrapper<V,D>)val.val.val;
            // single threaded variables will not throw backout exceptions.
            try {                
                casted_wrapper.append(
                    null, to_load, incorporating_deltas);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}