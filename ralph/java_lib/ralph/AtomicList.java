package ralph;

import java.util.ArrayList;
import java.util.Map.Entry;
import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto;


/**
 * @param <V>  ---- The type of each internal value in the internal arraylist
 * @param <D>  ---- The type that each value in the internal arraylist
 * 
 * A list of strings:
 * 
 * LockedListVariable<String,ArrayList<String>>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number, ArrayList<Number > >
 *     ArrayList<Number,ArrayList<Number>>>
 * 
 */
public abstract class AtomicList<V,D>
    extends AtomicListContainerReference<V,D>
{
    public AtomicList(
        String _host_uuid, boolean _peered,
        ArrayList<LockedObject<V,D>> init_val,boolean incorporating_deltas,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        super(
            _host_uuid,_peered,
            // initial value
            new AtomicInternalListVariable<V,D>(
                _host_uuid,false,locked_wrapper),
            // default value
            new AtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperConstructor<AtomicListContainer<V,D>,D>());

        load_init_vals(init_val,incorporating_deltas);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedList.
       This constructor is for this.
     */
    public AtomicList(
        String _host_uuid, boolean _peered,
        AtomicListContainer<V,D> internal_val,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        super(
            _host_uuid,_peered,
            // initial value
            internal_val,
            // default value
            new AtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperConstructor<AtomicListContainer<V,D>,D>());
    }
    
    public AtomicList(
        String _host_uuid, boolean _peered,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new AtomicInternalListVariable<V,D>(
                _host_uuid,false,locked_wrapper),
            // default value
            new AtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperConstructor<AtomicListContainer<V,D>,D>());
    }

    public void serialize_as_rpc_arg(
        ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        Util.logger_assert("FIXME: finish serializing lists.");
        // AtomicMapContainer<K,V,D> internal_val =
        //     get_val(active_event);
        // internal_val.serialize_as_rpc_arg(
        //     active_event,any_builder,is_reference);
    }

    public AtomicList(
        String _host_uuid, boolean _peered,
        ArrayList<LockedObject<V,D>> init_val,
        EnsureLockedWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new AtomicInternalListVariable<V,D>(
                _host_uuid,false,locked_wrapper),
            // default value
            new AtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperConstructor<AtomicListContainer<V,D>,D>());

        load_init_vals(init_val,false);
    }

    public void load_init_vals(
        ArrayList<LockedObject<V,D>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (LockedObject<V,D> to_load: init_val)
        {
            ListTypeDataWrapper<V,D>casted_wrapper = (ListTypeDataWrapper<V,D>)val.val.val;
            // single threaded variables will not throw backout exceptions.
            try {                
                casted_wrapper.append(
                    null, to_load, incorporating_deltas);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}