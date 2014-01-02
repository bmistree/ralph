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
public abstract class AtomicMap<K,V,D>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicInternalMap.
    // when call set val, must pass in a NonAtomicInternalMap
    AtomicInternalMap<K,V,D>, 
    // what will return when call de_waldoify.
    D>    
{

    public AtomicMap(
        String _host_uuid, boolean _peered,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super();

        AtomicInternalMap<K,V,D> init_val = new AtomicInternalMap<K,V,D>();
        init_val.init_multithreaded_map_container(
            _host_uuid, _peered,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        AtomicInternalMap<K,V,D> default_val = new AtomicInternalMap<K,V,D>();
        default_val.init_multithreaded_map_container(
            _host_uuid, _peered,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _host_uuid, _peered, init_val,default_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,D>,D>());
    }

    
    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedMap.
       This constructor is for this.
     */
    public AtomicMap(
        String _host_uuid, boolean _peered,
        AtomicInternalMap<K,V,D> internal_val,
        NonAtomicInternalMap.IndexType index_type,        
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super();
        
        AtomicInternalMap<K,V,D> default_val = new AtomicInternalMap<K,V,D>();
        default_val.init_multithreaded_map_container(
            _host_uuid, _peered,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _host_uuid, _peered, internal_val,default_val,
            new ValueTypeDataWrapperFactory<AtomicInternalMap<K,V,D>,D>());
    }

    
    public AtomicMap(
        String _host_uuid, boolean _peered,
        HashMap<K,RalphObject<V,D>> init_val,boolean incorporating_deltas,
        NonAtomicInternalMap.IndexType index_type,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super();

        AtomicInternalMap<K,V,D> init_val_2 = new AtomicInternalMap<K,V,D>();
        init_val_2.init_multithreaded_map_container(
            _host_uuid, _peered,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        AtomicInternalMap<K,V,D> default_val = new AtomicInternalMap<K,V,D>();
        default_val.init_multithreaded_map_container(
            _host_uuid, _peered,
            new MapTypeDataWrapperFactory<K,V,D>(),
            new HashMap<K,RalphObject<V,D>>(),
            index_type,
            locked_wrapper);

        init_atomic_value_variable(
            _host_uuid, _peered, init_val_2,default_val,
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
            }
        }
    }
}