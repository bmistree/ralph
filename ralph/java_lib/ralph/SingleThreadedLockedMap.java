package ralph;

import java.util.HashMap;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;
import java.util.Map.Entry;


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
public abstract class SingleThreadedLockedMap<K,V,D>
    extends SingleThreadedContainerReference<K,V,D>
{
    public SingleThreadedLockedMap(
        String _host_uuid, boolean _peered,
        HashMap<K,LockedObject<V,D>> init_val,boolean incorporating_deltas,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid,false,index_type,locked_wrapper),
            // default value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid, _peered,index_type,locked_wrapper),
            new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());

        load_init_vals(init_val,incorporating_deltas);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another SingleThreadedLockMap.
       This constructor is for this.
     */
    public SingleThreadedLockedMap(
        String _host_uuid, boolean _peered,
        SingleThreadedLockedContainer<K,V,D> internal_val,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        super(
            _host_uuid,_peered,
            // initial value
            internal_val,
            // default value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid, _peered,index_type,locked_wrapper),
            new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());
    }
    
    public SingleThreadedLockedMap(
        String _host_uuid, boolean _peered,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid,false,index_type,locked_wrapper),
            // default value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid, _peered,index_type,locked_wrapper),
            new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());
    }

    public void serialize_as_rpc_arg(
        ActiveEvent active_event,Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        SingleThreadedLockedContainer<K,V,D> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(
            active_event,any_builder,is_reference);
    }

    public SingleThreadedLockedMap(
        String _host_uuid, boolean _peered,
        HashMap<K,LockedObject<V,D>> init_val,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid,false,index_type,locked_wrapper),
            // default value
            new SingleThreadedLockedInternalMapVariable<K,V,D>(
                _host_uuid, _peered,index_type,locked_wrapper),
            new ValueTypeDataWrapperConstructor<SingleThreadedLockedContainer<K,V,D>,D>());

        load_init_vals(init_val,false);
    }

    public void load_init_vals(
        HashMap<K,LockedObject<V,D>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (Entry<K, LockedObject<V,D>> entry : init_val.entrySet())
        {
            ReferenceTypeDataWrapper<K,V,D>casted_wrapper = (ReferenceTypeDataWrapper<K,V,D>)val.val.val;

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