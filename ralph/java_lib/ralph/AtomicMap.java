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
 * @param <K>  ---- The keys used for indexing
 * @param <V>  ---- The type of each internal value in the internal hash map
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
public class AtomicMap<K,V,ValueDeltaType>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return NonAtomicInternalMap.  when call set val, must
    // pass in a NonAtomicInternalMap Note: version helper gets passed
    // in delta of this type.
    AtomicInternalMap<K,V,ValueDeltaType>
    >
{
    public final static String deserialization_label = "Atomic Map";
    
    private NonAtomicInternalMap.IndexType index_type = null;
    private EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper = null;
    
    public AtomicMap(
        boolean _log_changes,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalMap<K,V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalMap<K,V,ValueDeltaType> init_val =
            new AtomicInternalMap<K,V,ValueDeltaType>(ralph_globals);
        init_val.init_multithreaded_map_container(
            _log_changes,
            new MapTypeDataWrapperFactory<K,V,ValueDeltaType>(),
            new HashMap<K,RalphObject<V,ValueDeltaType>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,ValueDeltaType>>(),
            version_helper);
    }

    @Override
    protected
        // return type
        SpeculativeAtomicObject<
            AtomicInternalMap<K,V,ValueDeltaType>,
            AtomicInternalMap<K,V,ValueDeltaType>>
        // function name and arguments
        duplicate_for_speculation(
            AtomicInternalMap<K,V,ValueDeltaType> to_speculate_on)
    {
        SpeculativeAtomicObject<
            AtomicInternalMap<K,V,ValueDeltaType>,
            AtomicInternalMap<K,V,ValueDeltaType>> to_return =
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
    
    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedMap.
       This constructor is for this.
     */
    public AtomicMap(
        boolean _log_changes,
        AtomicInternalMap<K,V,ValueDeltaType> internal_val,
        NonAtomicInternalMap.IndexType index_type,        
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalMap<K,V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;

        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,ValueDeltaType>>(),
            version_helper);
    }

    
    public AtomicMap(
        boolean _log_changes,
        HashMap<K,RalphObject<V,ValueDeltaType>> init_val,boolean incorporating_deltas,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<AtomicInternalMap<K,V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;
        AtomicInternalMap<K,V,ValueDeltaType> init_val_2 =
            new AtomicInternalMap<K,V,ValueDeltaType>(ralph_globals);
        init_val_2.init_multithreaded_map_container(
            _log_changes,
            new MapTypeDataWrapperFactory<K,V,ValueDeltaType>(),
            new HashMap<K,RalphObject<V,ValueDeltaType>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val_2,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,ValueDeltaType>>(),
            version_helper);

        load_init_vals(init_val,incorporating_deltas);
    }
    
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,
        VariablesProto.Variables.Any.Builder any_builder)
        throws BackoutException
    {
        AtomicInternalMap<K,V,ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
        any_builder.setIsTvar(true);
    }
    
    public void load_init_vals(
        Map<K,RalphObject<V,ValueDeltaType>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (Entry<K, RalphObject<V,ValueDeltaType>> entry : init_val.entrySet())
        {
            MapTypeDataWrapper<K,V,ValueDeltaType>casted_wrapper =
                (MapTypeDataWrapper<K,V,ValueDeltaType>)val.val.val;

            // single threaded variables will not throw backout exceptions.
            try {
                casted_wrapper.set_val_on_key(
                    null, entry.getKey(), entry.getValue(), incorporating_deltas);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Util.logger_assert("Did not consider effect of backout in init");
            }
        }
    }
}