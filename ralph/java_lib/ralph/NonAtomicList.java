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
    extends NonAtomicVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicInternalList.
    // when call set val, must pass in a NonAtomicInternalList
    // Note: the type that the version helper for this nonatomic list gets
    // passed.
    NonAtomicInternalList<ValueType,DeltaType>,
    IReference>
{
    public final static String deserialization_label = "NonAtomic List";
    
    public NonAtomicList(
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<ValueType> value_type_class,
        RalphGlobals ralph_globals)
    {
        this(
            new NonAtomicInternalList<ValueType,DeltaType>(ralph_globals),
            locked_wrapper,version_helper,value_type_class,ralph_globals);
        
        this.val.val.init(
            new ListTypeDataWrapperFactory<ValueType,DeltaType>(value_type_class),
            new ArrayList<RalphObject<ValueType,DeltaType>>(),locked_wrapper);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicLockList.
       This constructor is for this.
     */
    public NonAtomicList(
        NonAtomicInternalList<ValueType,DeltaType> internal_val,
        EnsureAtomicWrapper<ValueType,DeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<ValueType> value_type_class,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_non_atomic_value_variable(
            internal_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<ValueType,DeltaType>>(),
            version_helper);
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
}