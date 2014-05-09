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
public class AtomicList<V,D>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return AtomicListContainer.
    // when call set val, must pass in a AtomicListContainer
    AtomicInternalList<V,D>, 
    // what will return when call de_waldoify.
    D>
{
    private EnsureAtomicWrapper<V,D> locked_wrapper = null;

    public final static String deserialization_label = "Atomic List";
    
    public AtomicList(
        boolean _log_changes,EnsureAtomicWrapper<V,D> locked_wrapper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<V,D> init_val =
            new AtomicInternalList<V,D>(ralph_globals);
        init_val.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,D>,D>());
    }
    

    @Override
    protected SpeculativeAtomicObject<AtomicInternalList<V,D>, D>
        duplicate_for_speculation(AtomicInternalList<V,D> to_speculate_on)
    {
        SpeculativeAtomicObject<AtomicInternalList<V,D>, D> to_return =
            new AtomicList(
                log_changes,
                to_speculate_on,
                locked_wrapper,
                ralph_globals);
        
        to_return.set_derived(this);
        return to_return;
    }

    
    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedList.
       This constructor is for this.
     */
    public AtomicList(
        boolean _log_changes,
        AtomicInternalList<V,D> internal_val,
        EnsureAtomicWrapper<V,D> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,D>,D>());
    }

    
    public AtomicList(
        boolean _log_changes,
        ArrayList<RalphObject<V,D>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<V,D> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<V,D> init_val_2 =
            new AtomicInternalList<V,D>(ralph_globals);
        init_val_2.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<V,D>(),
            new ArrayList<RalphObject<V,D>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val_2,
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
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        AtomicInternalList<V,D> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
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
                Util.logger_assert(
                    "Did not consider effect of backout in load");
            }
        }
    }
}