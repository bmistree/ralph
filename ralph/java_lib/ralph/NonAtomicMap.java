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
public abstract class NonAtomicMap<K,V,ValueDeltaType>
    extends NonAtomicValueVariable<
    // this wraps a locked container object.  Ie, calling get_val on
    // this will return NonAtomicInternalMap.  when call set val, must
    // pass in a NonAtomicInternalMap Note: this is the type that is
    // sent into the version helper for logging.
    NonAtomicInternalMap<K,V,ValueDeltaType>
    >
{
    public final static String deserialization_label = "NonAtomic Map";
    
    public NonAtomicMap(
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<NonAtomicInternalMap<K,V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(ralph_globals);
        
        NonAtomicInternalMap<K,V,ValueDeltaType> init_val =
            new NonAtomicInternalMap<K,V,ValueDeltaType>(ralph_globals);
        init_val.init(
            new MapTypeDataWrapperFactory<K,V,ValueDeltaType>(),
            new HashMap<K,RalphObject<V,ValueDeltaType>>(),
            index_type,
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalMap<K,V,ValueDeltaType>>(),
            version_helper);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicInternalMap.
       This constructor is for this.
     */
    public NonAtomicMap(
        NonAtomicInternalMap<K,V,ValueDeltaType> internal_val,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<NonAtomicInternalMap<K,V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_non_atomic_value_variable(
            internal_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalMap<K,V,ValueDeltaType>>(),
            version_helper);
    }

    public NonAtomicMap(
        Map<K,RalphObject<V,ValueDeltaType>> init_val,boolean incorporating_deltas,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,ValueDeltaType> locked_wrapper,
        VersionHelper<NonAtomicInternalMap<K,V,ValueDeltaType>> version_helper,
        RalphGlobals ralph_globals)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			        
        super(ralph_globals);
        
        NonAtomicInternalMap<K,V,ValueDeltaType> init_val_2 =
            new NonAtomicInternalMap<K,V,ValueDeltaType>(ralph_globals);
        init_val_2.init(
            new MapTypeDataWrapperFactory<K,V,ValueDeltaType>(),
            new HashMap<K,RalphObject<V,ValueDeltaType>>(),
            index_type,
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val_2,
            new ValueTypeDataWrapperFactory<NonAtomicInternalMap<K,V,ValueDeltaType>>(),
            version_helper);

        load_init_vals(init_val,incorporating_deltas);
    }

    
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,Variables.Any.Builder any_builder)
        throws BackoutException
    {
        NonAtomicInternalMap<K,V,ValueDeltaType> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
        any_builder.setIsTvar(false);
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
            try
            {
                casted_wrapper.set_val_on_key(
                    null, entry.getKey(), entry.getValue(),
                    incorporating_deltas);
            }
            catch (BackoutException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Util.logger_assert(
                    "Did not consider effect of backout when loading");                
            }
        }
    }
}