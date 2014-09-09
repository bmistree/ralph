package ralph;

import java.util.List;
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
 * 
 * A list of strings:
 * 
 * LockedListVariable<String>
 * 
 * A list of lists of numbers:
 * 
 * LockedListVariable<
 *     LockedListVariable< Number, ArrayList<Number > > >
 * 
 */
public abstract class NonAtomicList<V>
    extends NonAtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return NonAtomicInternalList.
    // when call set val, must pass in a NonAtomicInternalList
    NonAtomicInternalList<V>>

{
    public final static String deserialization_label = "NonAtomic List";
    
    public NonAtomicList(
        EnsureAtomicWrapper<V> locked_wrapper, RalphGlobals ralph_globals)
    {
        // FIXME: I'm pretty sure that the type signature for the locked object above
        // is incorrect: it shouldn't be D, right?			
        super(ralph_globals);
        
        NonAtomicInternalList<V> init_val =
            new NonAtomicInternalList<V>(ralph_globals);
        init_val.init(
            new ListTypeDataWrapperFactory<V>(),
            new ArrayList<RalphObject<V>>(),
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<V>>());
    }

    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another NonAtomicLockList.
       This constructor is for this.
     */
    public NonAtomicList(
        NonAtomicInternalList<V> internal_val,
        EnsureAtomicWrapper<V> locked_wrapper, RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        
        init_non_atomic_value_variable(
            internal_val,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<V>>());
    }

    public NonAtomicList(
        List<RalphObject<V>> init_val,boolean incorporating_deltas,
        EnsureAtomicWrapper<V> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        
        NonAtomicInternalList<V> init_val_2 =
            new NonAtomicInternalList<V>(ralph_globals);
        init_val_2.init(
            new ListTypeDataWrapperFactory<V>(),
            new ArrayList<RalphObject<V>>(),
            locked_wrapper);
        
        init_non_atomic_value_variable(
            init_val_2,
            new ValueTypeDataWrapperFactory<NonAtomicInternalList<V>>());

        load_init_vals(init_val,incorporating_deltas);
    }


    public void serialize_as_rpc_arg(
        ActiveEvent active_event,
        Variables.Any.Builder any_builder) throws BackoutException
    {
        NonAtomicInternalList<V> internal_val =
            get_val(active_event);
        internal_val.serialize_as_rpc_arg(active_event,any_builder);
    }

    public boolean return_internal_val_from_container()
    {
        return false;
    }
    
    public void load_init_vals(
        List<RalphObject<V>> init_val, boolean incorporating_deltas)
    {
        if (init_val == null)
            return;

        //FIXME probably inefficient to add each field separately
        for (RalphObject<V> to_load: init_val)
        {
            ListTypeDataWrapper<V>casted_wrapper = (ListTypeDataWrapper<V>)val.val.val;
            // single threaded variables will not throw backout exceptions.
            try {                
                casted_wrapper.append(
                    null, to_load, incorporating_deltas);
            } catch (BackoutException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Util.logger_assert(
                    "Did not consider effect of backout when loading");
            }
        }
    }
}