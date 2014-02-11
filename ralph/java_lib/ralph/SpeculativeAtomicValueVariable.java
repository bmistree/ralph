package ralph;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.DataWrapper;

public abstract class SpeculativeAtomicValueVariable<T,D> extends SpeculativeAtomicObject<T,D> 
{
    public SpeculativeAtomicValueVariable(
        boolean _log_changes, T init_val,T default_value,
        ValueTypeDataWrapperFactory<T,D> vtdwc,RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_multithreaded_locked_object(vtdwc,_log_changes,init_val);
    }

    public SpeculativeAtomicValueVariable(RalphGlobals ralph_globals)
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
    public void write_if_different(
        ActiveEvent active_event,
        Object new_val) throws BackoutException 
    {
        DataWrapper<T, D> to_write_on = acquire_write_lock(active_event,null);
        to_write_on.write((T)new_val,true);		
    }

    @Override
    public boolean return_internal_val_from_container() 
    {
        return true;
    }
}
