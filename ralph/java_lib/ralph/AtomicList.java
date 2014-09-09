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
 *
 * A list of strings:
 * 
 * LockedListVariable<String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number > >
 * 
 */
public class AtomicList<V>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return AtomicListContainer.
    // when call set val, must pass in a AtomicListContainer
    AtomicInternalList<V>>
{
    private EnsureAtomicWrapper<V> locked_wrapper = null;

    public final static String deserialization_label = "Atomic List";
    
    public AtomicList(
        boolean _log_changes,EnsureAtomicWrapper<V> locked_wrapper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<V> init_val =
            new AtomicInternalList<V>(ralph_globals);
        init_val.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<V>(),
            new ArrayList<RalphObject<V>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V>>());
    }
    

    @Override
    protected SpeculativeAtomicObject<AtomicInternalList<V>>
        duplicate_for_speculation(AtomicInternalList<V> to_speculate_on)
    {
        SpeculativeAtomicObject<AtomicInternalList<V>> to_return =
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
        AtomicInternalList<V> internal_val,
        EnsureAtomicWrapper<V> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V>>());
    }

    
    public AtomicList(
        boolean _log_changes,
        ArrayList<RalphObject<V>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<V> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<V> init_val_2 =
            new AtomicInternalList<V>(ralph_globals);
        init_val_2.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<V>(),
            new ArrayList<RalphObject<V>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val_2,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V>>());

        load_init_vals(init_val,incorporating_deltas);
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
        AtomicInternalList<V> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
    }

    public void load_init_vals(
        ArrayList<RalphObject<V>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<V> to_load: init_val)
        {
            ListTypeDataWrapper<V>casted_wrapper = (ListTypeDataWrapper<V>)val.val.val;
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