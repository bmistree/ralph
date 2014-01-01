package ralph;

import java.util.ArrayList;
import ralph_protobuffs.VariablesProto.Variables;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;

/**
 * @param <V>  ---- The type of each internal value in the internal arraylist
 * @param <D>  ---- The type that each value in the internal arraylist
 * 
 * A list of strings:
 * 
 * LockedListVariable<String,String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number, ArrayList<Number > >
 *     ArrayList<Number,ArrayList<Number>>>
 * 
 */
public abstract class NonAtomicList<V,D>
    extends NonAtomicListContainerReference<V,D>
{
    public NonAtomicList(
        String _host_uuid, boolean _peered,
        ArrayList<RalphObject<V,D>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid,false,locked_wrapper),
            // default value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperFactory<NonAtomicListContainer<V,D>,D>());

        load_init_vals(init_val,incorporating_deltas);
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicLockList.
       This constructor is for this.
     */
    public NonAtomicList(
        String _host_uuid, boolean _peered,
        NonAtomicListContainer<V,D> internal_val,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        super(
            _host_uuid,_peered,
            // initial value
            internal_val,
            // default value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperFactory<NonAtomicListContainer<V,D>,D>());
    }
    
    public NonAtomicList(
        String _host_uuid, boolean _peered,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid,false,locked_wrapper),
            // default value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperFactory<NonAtomicListContainer<V,D>,D>());
    }

    public void serialize_as_rpc_arg(
        ActiveEvent active_event,Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        NonAtomicListContainer<V,D> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(
            active_event,any_builder,is_reference);
    }

    public NonAtomicList(
        String _host_uuid, boolean _peered,
        ArrayList<RalphObject<V,D>> init_val,
        EnsureAtomicWrapper<V,D> locked_wrapper)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(
            _host_uuid,_peered,
            // initial value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid,false,locked_wrapper),
            // default value
            new NonAtomicInternalListVariable<V,D>(
                _host_uuid, _peered,locked_wrapper),
            new ValueTypeDataWrapperFactory<NonAtomicListContainer<V,D>,D>());

        load_init_vals(init_val,false);
    }

    public void load_init_vals(
        ArrayList<RalphObject<V,D>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<V,D> to_load: init_val)
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