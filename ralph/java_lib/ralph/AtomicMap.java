package ralph;

import java.util.HashMap;
import java.util.Map;
import ralph_protobuffs.VariablesProto;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;

/**
 * @param <KeyType>  ---- The keys used for indexing
 * @param <ValueType>  ---- The type of each internal value in the internal hash map
 *
 * A map of numbers to strings:
 * 
 * LockedMapVariable<Number,String,String>
 * 
 * A map of numbers to maps of numbers to strings
 * 
 * LockedMapVariable<
 *     Number,
 *     LockedMapVariable< Number, String >,  >
 * 
 */
public class AtomicMap<KeyType,ValueType,ValueDeltaType>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return NonAtomicInternalMap.  when call set val, must
    // pass in a NonAtomicInternalMap Note: version helper gets passed
    // in delta of this type.
    AtomicInternalMap<KeyType,ValueType,ValueDeltaType>
    >
{
    public final static String deserialization_label = "Atomic Map";
    
    private NonAtomicInternalMap.IndexType index_type = null;
    private EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper = null;
    
    public AtomicMap(
        boolean _log_changes,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalMap<KeyType,ValueType,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        this(
            _log_changes,
            new AtomicInternalMap<KeyType,ValueType,ValueDeltaType>(ralph_globals),
            index_type,locked_wrapper,version_helper,ralph_globals);
        this.val.val.init_multithreaded_map_container(
            _log_changes,
            new MapTypeDataWrapperFactory<KeyType,ValueType,ValueDeltaType>(),
            new HashMap<KeyType,RalphObject<ValueType,ValueDeltaType>>(),
            index_type,
            locked_wrapper);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedMap.
       This constructor is for this.
     */
    public AtomicMap(
        boolean _log_changes,
        AtomicInternalMap<KeyType,ValueType,ValueDeltaType> internal_val,
        NonAtomicInternalMap.IndexType index_type,        
        EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalMap<KeyType,ValueType,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;

        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<KeyType,ValueType,ValueDeltaType>>(),
            version_helper);
    }

    
    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            AtomicInternalMap<KeyType,ValueType,ValueDeltaType>,
            AtomicInternalMap<KeyType,ValueType,ValueDeltaType>>
        // function name and arguments
        duplicate_for_speculation(
            AtomicInternalMap<KeyType,ValueType,ValueDeltaType> to_speculate_on)
    {
        SpeculativeAtomicObject<
            AtomicInternalMap<KeyType,ValueType,ValueDeltaType>,
            AtomicInternalMap<KeyType,ValueType,ValueDeltaType>> to_return =
            new AtomicMap(
                log_changes,
                to_speculate_on,
                index_type,
                locked_wrapper,
                version_helper,
                ralph_globals);

        
        to_return.set_derived(this);
        return to_return;
    }
    
    
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        AtomicInternalMap<KeyType,ValueType,ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
        any_builder.setIsTvar(true);
    }
}