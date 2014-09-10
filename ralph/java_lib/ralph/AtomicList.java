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
public class AtomicList<ValueType, ValueDeltaType>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return AtomicListContainer.  when call set val, must
    // pass in a AtomicListContainer.  Note:version helper gets passed
    // in delta of this type.
    AtomicInternalList<ValueType, ValueDeltaType>
    >
{
    private EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper = null;

    public final static String deserialization_label = "Atomic List";
    private final Class<ValueType> value_type_class;
    
    public AtomicList(
        boolean _log_changes,EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalList<ValueType, ValueDeltaType>> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalList<ValueType, ValueDeltaType> init_val =
            new AtomicInternalList<ValueType, ValueDeltaType>(ralph_globals);
        init_val.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<ValueType, ValueDeltaType>(_value_type_class),
            new ArrayList<RalphObject<ValueType, ValueDeltaType>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<ValueType, ValueDeltaType>>(),
            version_helper);

        value_type_class = _value_type_class;
    }
    
    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            AtomicInternalList<ValueType, ValueDeltaType>,
            AtomicInternalList<ValueType, ValueDeltaType>>
        // function name and arguments
        duplicate_for_speculation(AtomicInternalList<ValueType, ValueDeltaType> to_speculate_on)
    {
        SpeculativeAtomicObject<
            AtomicInternalList<ValueType, ValueDeltaType>,
            AtomicInternalList<ValueType, ValueDeltaType>> to_return =
            new AtomicList(
                log_changes,
                to_speculate_on,
                locked_wrapper,
                version_helper,
                value_type_class,
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
        AtomicInternalList<ValueType, ValueDeltaType> internal_val,
        EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalList<ValueType, ValueDeltaType>> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;
        
        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalList<ValueType, ValueDeltaType>>(),
            version_helper);
        value_type_class = _value_type_class;
    }

    
    public AtomicList(
        boolean _log_changes,
        List<RalphObject<ValueType, ValueDeltaType>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<ValueType, ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalList<ValueType, ValueDeltaType>> version_helper,
        Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.locked_wrapper = locked_wrapper;

        // FIXME: use delegating constructors instead.
        
        AtomicInternalList<ValueType, ValueDeltaType> init_val_2 =
            new AtomicInternalList<ValueType, ValueDeltaType>(ralph_globals);
        init_val_2.init_multithreaded_list_container(
            _log_changes,
            new ListTypeDataWrapperFactory<ValueType, ValueDeltaType>(_value_type_class),
            new ArrayList<RalphObject<ValueType, ValueDeltaType>>(),
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val_2,
            new ValueTypeDataWrapperFactory<AtomicInternalList<ValueType, ValueDeltaType>>(),
            version_helper);

        load_init_vals(init_val,incorporating_deltas);
        value_type_class = _value_type_class;
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
        AtomicInternalList<ValueType, ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
    }

    public void load_init_vals(
        List<RalphObject<ValueType, ValueDeltaType>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<ValueType, ValueDeltaType> to_load: init_val)
        {
            ListTypeDataWrapper<ValueType, ValueDeltaType>casted_wrapper =
                (ListTypeDataWrapper<ValueType, ValueDeltaType>)val.val.val;
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