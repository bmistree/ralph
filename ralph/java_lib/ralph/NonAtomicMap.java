package ralph;

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
public abstract class NonAtomicMap<K,V,D>
    extends NonAtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicInternalMap.
    // when call set val, must pass in a NonAtomicInternalMap
    NonAtomicInternalMap<K,V,D>, 
    // what will return when call de_waldoify.
    D>
{
    public final static String deserialization_label = "NonAtomic Map";
    
    public NonAtomicMap(
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper,RalphGlobals ralph_globals)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(ralph_globals);
        
        NonAtomicInternalMap<K,V,D> init_val =
            new NonAtomicInternalMap<K,V,D>(ralph_globals);
        init_val.init(
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalMap<K,V,D>,D>());
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicInternalMap.
       This constructor is for this.
     */
    public NonAtomicMap(
        NonAtomicInternalMap<K,V,D> internal_val,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        
        init_non_atomic_value_variable(
            internal_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalMap<K,V,D>,D>());
    }

    public NonAtomicMap(
        HashMap<K,RalphObject<V,D>> init_val,boolean incorporating_deltas,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper,RalphGlobals ralph_globals)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			        
        super(ralph_globals);
        
        NonAtomicInternalMap<K,V,D> init_val_2 =
            new NonAtomicInternalMap<K,V,D>(ralph_globals);
        init_val_2.init(
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val_2,
            new ValueTypeDataWrapperFactory<NonAtomicInternalMap<K,V,D>,D>());

        load_init_vals(init_val,incorporating_deltas);
    }

    
    public void serialize_as_rpc_arg(
        ActiveEvent active_event,Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        NonAtomicInternalMap<K,V,D> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(
            active_event,any_builder,is_reference);
        any_builder.setIsTvar(false);
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
                Util.logger_assert(
                    "Did not consider effect of backout when loading");                
            }
        }
    }
}