package ralph;

import java.util.HashMap;
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
 * @param <D>  ---- The type that each value in the internal hash map would dewaldoify into
 * 
 * A map of numbers to strings:
 * 
 * LockedMapVariable<Number,String,HashMap<String,Number>>
 * 
 * A map of numbers to maps of numbers to strings
 * 
 * LockedMapVariable<
 *     Number,
 *     LockedMapVariable< Number, String, HashMap<String,Number > >
 *     HashMap<Number,HashMap<String,Number>>>
 * 
 */
public class AtomicMap<K,V,D>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicInternalMap.
    // when call set val, must pass in a NonAtomicInternalMap
    AtomicInternalMap<K,V,D>, 
    // what will return when call de_waldoify.
    D>    
{
    public final static String deserialization_label = "Atomic Map";
    
    private NonAtomicInternalMap.IndexType index_type = null;
    private EnsureAtomicWrapper<V,D> locked_wrapper = null;
    
    public AtomicMap(
        boolean _log_changes,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;
        
        AtomicInternalMap<K,V,D> init_val =
            new AtomicInternalMap<K,V,D>(ralph_globals);
        init_val.init_multithreaded_map_container(
            _log_changes,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,D>,D>());
    }

    @Override
    protected SpeculativeAtomicObject<AtomicInternalMap<K,V,D>, D>
        duplicate_for_speculation(AtomicInternalMap<K,V,D> to_speculate_on)
    {
        SpeculativeAtomicObject<AtomicInternalMap<K,V,D>, D> to_return =
            new AtomicMap(
                log_changes,
                to_speculate_on,
                index_type,
                locked_wrapper,
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
        AtomicInternalMap<K,V,D> internal_val,
        NonAtomicInternalMap.IndexType index_type,        
        EnsureAtomicWrapper<V,D> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;

        init_atomic_value_variable(
            _log_changes, internal_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,D>,D>());
    }

    
    public AtomicMap(
        boolean _log_changes,
        HashMap<K,RalphObject<V,D>> init_val,boolean incorporating_deltas,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper, RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        this.index_type = index_type;
        this.locked_wrapper = locked_wrapper;
        AtomicInternalMap<K,V,D> init_val_2 =
            new AtomicInternalMap<K,V,D>(ralph_globals);
        init_val_2.init_multithreaded_map_container(
            _log_changes,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _log_changes, init_val_2,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,D>,D>());

        load_init_vals(init_val,incorporating_deltas);
    }
    
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        AtomicInternalMap<K,V,D> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(
            active_event,any_builder,is_reference);
        any_builder.setIsTvar(true);
    }
    
    public void load_init_vals(
        HashMap<K,RalphObject<V,D>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (Entry<K, RalphObject<V,D>> entry : init_val.entrySet())
        {
            MapTypeDataWrapper<K,V,D>casted_wrapper = (MapTypeDataWrapper<K,V,D>)val.val.val;

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