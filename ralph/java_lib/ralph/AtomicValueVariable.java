package ralph;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.DataWrapper;

public abstract class AtomicValueVariable<T,D> extends SpeculativeAtomicObject<T,D> 
{
    public AtomicValueVariable(
        boolean _log_changes, T init_val,T default_value,
        ValueTypeDataWrapperFactory<T,D> vtdwc,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_multithreaded_locked_object(vtdwc,_log_changes,init_val);
    }

    public AtomicValueVariable(RalphGlobals ralph_globals)
    {
        super (ralph_globals);
    }

    public void init_atomic_value_variable(
        boolean _log_changes, T init_val,T default_value,
        ValueTypeDataWrapperFactory<T,D> vtdwc)
    {
        init_multithreaded_locked_object(vtdwc,_log_changes,init_val);
    }
    
    
    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        this.set_val(active_event,(T)to_swap_with.get_val(active_event));
    }
    
    @Override
    public boolean return_internal_val_from_container() 
    {
        return true;
    }
}
