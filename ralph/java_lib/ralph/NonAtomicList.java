package ralph;

import java.util.List;
import java.util.ArrayList;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;

/**
 * @param <V> ---- The type of each internal value in the internal
 * list
 * 
 * A list of strings:
 * 
 * LockedListVariable<String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number, List<Number > > >
 * 
 */
public abstract class NonAtomicList<ValueType,DeltaType>
    extends NonAtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicInternalList.
    // when call set val, must pass in a NonAtomicInternalList
    // Note: the type that the version helper for this nonatomic list gets
    // passed.
    NonAtomicInternalList<ValueType,DeltaType>
    >
{
    public final static String deserialization_label = "NonAtomic List";
    
    public NonAtomicList(
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<
            NonAtomicInternalList<ValueType,DeltaType>> version_helper,
        Class<ValueType> value_type_class,
        RalphGlobals ralph_globals)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(ralph_globals);
        
        NonAtomicInternalList<ValueType,DeltaType> init_val =
            new NonAtomicInternalList<ValueType,DeltaType>(ralph_globals);

        init_val.init(
            new ListTypeDataWrapperFactory<ValueType,DeltaType>(value_type_class),
            new ArrayList<RalphObject<ValueType,DeltaType>>(),locked_wrapper);

        init_non_atomic_value_variable(
            init_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<ValueType,DeltaType>>(),
            version_helper);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicLockList.
       This constructor is for this.
     */
    public NonAtomicList(
        NonAtomicInternalList<ValueType,DeltaType> internal_val,
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<
            NonAtomicInternalList<ValueType,DeltaType>> version_helper,
        Class<ValueType> value_type_class,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        
        init_non_atomic_value_variable(
            internal_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<ValueType,DeltaType>>(),
            version_helper);
    }

    public NonAtomicList(
        List<RalphObject<ValueType,DeltaType>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<
            NonAtomicInternalList<ValueType,DeltaType>> version_helper,
        Class<ValueType> value_type_class,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        
        NonAtomicInternalList<ValueType,DeltaType> init_val_2 =
            new NonAtomicInternalList<ValueType,DeltaType>(ralph_globals);
        init_val_2.init(
            new ListTypeDataWrapperFactory<ValueType,DeltaType>(value_type_class),
            new ArrayList<RalphObject<ValueType,DeltaType>>(),
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val_2,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<ValueType,DeltaType>>(),
            version_helper);

        load_init_vals(init_val,incorporating_deltas);
    }


    public void serialize_as_rpc_arg(
        ActiveEvent active_event,
        Variables.Any.Builder any_builder) throws BackoutException
    {
        NonAtomicInternalList<ValueType,DeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
    }

    public boolean return_internal_val_from_container()
    {
        return false;
    }
    
    public void load_init_vals(
        List<RalphObject<ValueType,DeltaType>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<ValueType,DeltaType> to_load: init_val)
        {
            ListTypeDataWrapper<ValueType,DeltaType>casted_wrapper =
                (ListTypeDataWrapper<ValueType,DeltaType>)val.val.val;
            // single threaded variables will not throw backout exceptions.
            try {                
                casted_wrapper.append(
                    null, to_load, incorporating_deltas);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Util.logger_assert(
                    "Did not consider effect of backout when loading");
            }
        }
    }
}