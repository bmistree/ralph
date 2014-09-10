package ralph;

import java.util.ArrayList;
import java.util.List;
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
public class AtomicList<V,ValueDeltaType>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return AtomicListContainer.  when call set val, must
    // pass in a AtomicListContainer.  Note:version helper gets passed
    // in delta of this type.
    AtomicInternalList<V,ValueDeltaType>
    >
{
    private EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;

    public final static String deserialization_label = "Atomic List";
    
    public AtomicList(
        boolean _log_changes,EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalList<V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<V,ValueDeltaType> init_val =
            new AtomicInternalList<V,ValueDeltaType>(ralph_globals);
        init_val.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<V,ValueDeltaType>(),
            new ArrayList<RalphObject<V,ValueDeltaType>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,ValueDeltaType>>(),
            version_helper);
    }
    
    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            AtomicInternalList<V,ValueDeltaType>,
            AtomicInternalList<V,ValueDeltaType>>
        // function name and arguments
        duplicate_for_speculation(AtomicInternalList<V,ValueDeltaType> to_speculate_on)
    {
        SpeculativeAtomicObject<
            AtomicInternalList<V,ValueDeltaType>,
            AtomicInternalList<V,ValueDeltaType>> to_return =
            new AtomicList(
                log_changes,
                to_speculate_on,
                locked_wrapper,
                version_helper,
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
        AtomicInternalList<V,ValueDeltaType> internal_val,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalList<V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,ValueDeltaType>>(),
            version_helper);
    }

    
    public AtomicList(
        boolean _log_changes,
        List<RalphObject<V,ValueDeltaType>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalList<V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<V,ValueDeltaType> init_val_2 =
            new AtomicInternalList<V,ValueDeltaType>(ralph_globals);
        init_val_2.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<V,ValueDeltaType>(),
            new ArrayList<RalphObject<V,ValueDeltaType>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val_2,
            new ValueTypeDataWrapperFactory<AtomicInternalList<V,ValueDeltaType>>(),
            version_helper);

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
        AtomicInternalList<V,ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
    }

    public void load_init_vals(
        List<RalphObject<V,ValueDeltaType>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<V,ValueDeltaType> to_load: init_val)
        {
            ListTypeDataWrapper<V,ValueDeltaType>casted_wrapper =
                (ListTypeDataWrapper<V,ValueDeltaType>)val.val.val;
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