package ralph;

import java.util.ArrayList;
import java.util.Map.Entry;
import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;



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
public abstract class SpeculativeAtomicList<V,D>
    extends AtomicValueVariable<
    // this wraps a locked container object.  Ie,
    // calling get_val on this will return AtomicListContainer.
    // when call set val, must pass in a AtomicListContainer
    SpeculativeAtomicInternalList<V,D>, 
    // what will return when call de_waldoify.
    D>
{
    
    /**
       When pass an argument into a method call, should unwrap
       internal value and put it into another MultiThreadedList.
       This constructor is for this.
     */
    public SpeculativeAtomicList(
        boolean _log_changes,
        SpeculativeAtomicInternalList<V,D> internal_val,
        EnsureAtomicWrapper<V,D> locked_wrapper,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        
        init_atomic_value_variable(
            _log_changes, internal_val,
            // using null as default val because default_val is never
            // used in the future.
            null, 
            new ValueTypeDataWrapperFactory<SpeculativeAtomicInternalList<V,D>,D>());
    }

    
    public D de_waldoify(ActiveEvent active_event) throws BackoutException
    {
        Util.logger_warn("Must acquire read lock when de waldoifying.");
        return val.de_waldoify(active_event);
    }

    public boolean return_internal_val_from_container()
    {
        return false;
    }


    public void serialize_as_rpc_arg(
        ActiveEvent active_event,VariablesProto.Variables.Any.Builder any_builder,
        boolean is_reference) throws BackoutException
    {
        Util.logger_assert("FIXME: finish serializing lists.");
    }
}