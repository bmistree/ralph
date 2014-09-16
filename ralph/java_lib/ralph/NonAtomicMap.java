package ralph;

import java.util.Map;
import java.util.HashMap;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.MapTypeDataWrapperFactory;
import RalphDataWrappers.MapTypeDataWrapper;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

/**
 * @param <K>  ---- The keys used for indexing
 * @param <V> ---- The type of each internal value in the internal
 * map
 * 
 * A map of numbers to strings:
 * 
 * LockedMapVariable<Number,String>
 * 
 * A map of numbers to maps of numbers to strings
 * 
 * LockedMapVariable<
 *     Number,
 *     LockedMapVariable< Number, String>>
 */
public abstract class NonAtomicMap<KeyType,ValueType,ValueDeltaType>
    extends NonAtomicVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return NonAtomicInternalMap.  when call set val, must
    // pass in a NonAtomicInternalMap Note: this is the type that is
    // sent into the version helper for logging.
    NonAtomicInternalMap<KeyType,ValueType,ValueDeltaType>,
    IReference
    >
{
    public final static String deserialization_label = "NonAtomic Map";

    private final Class<KeyType> key_type_class;
    private final Class<ValueType> value_type_class;

    
    public NonAtomicMap(
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        InternalContainerTypeVersionHelper<KeyType> internal_version_helper,
        Class<KeyType> _key_type_class,Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        this(
            new NonAtomicInternalMap<KeyType,ValueType,ValueDeltaType>(
                ralph_globals,internal_version_helper),
            index_type,locked_wrapper,version_helper,_key_type_class,
            _value_type_class,ralph_globals);

        this.val.val.init(
            new MapTypeDataWrapperFactory<KeyType,ValueType,ValueDeltaType>(
                _key_type_class,_value_type_class),
            new HashMap<KeyType,RalphObject<ValueType,ValueDeltaType>>(),
            index_type,
            locked_wrapper);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicInternalMap.
       This constructor is for this.
     */
    public NonAtomicMap(
        NonAtomicInternalMap<KeyType,ValueType,ValueDeltaType> internal_val,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<ValueType,ValueDeltaType> locked_wrapper,
        VersionHelper<IReference> version_helper,
        Class<KeyType> _key_type_class,Class<ValueType> _value_type_class,
        RalphGlobals ralph_globals)
    {
        super(
            internal_val,
            new ValueTypeDataWrapperFactory<
                NonAtomicInternalMap<KeyType,ValueType,ValueDeltaType>>(),
            version_helper,ralph_globals);

        key_type_class = _key_type_class;
        value_type_class = _value_type_class;
    }

    @Override
    public ObjectContents serialize_contents(ActiveEvent active_event)
        throws BackoutException
    {
        NonAtomicInternalMap<KeyType,ValueType,ValueDeltaType> internal_map = 
            get_val(active_event);

        return AtomicMap.serialize_map_reference(
            uuid(),internal_map.uuid(),key_type_class.getName(),
            value_type_class.getName(),false);
    }

    @Override
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,Variables.Any.Builder any_builder)
        throws BackoutException
    {
        NonAtomicInternalMap<KeyType,ValueType,ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
        any_builder.setIsTvar(false);
    }

}