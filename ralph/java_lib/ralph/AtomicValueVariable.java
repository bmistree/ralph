package ralph;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.DataWrapper;

public abstract class AtomicValueVariable<T,D> extends AtomicObject<T,D> 
{
    public AtomicValueVariable(
        String _host_uuid, boolean _log_changes, T init_val,T default_value,
        ValueTypeDataWrapperFactory<T,D> vtdwc)
    {
        super();
        init_multithreaded_locked_object(vtdwc,_host_uuid,_log_changes,init_val);
    }

    public AtomicValueVariable(){}

    public void init_atomic_value_variable(
        String _host_uuid, boolean _log_changes, T init_val,T default_value,
        ValueTypeDataWrapperFactory<T,D> vtdwc)
    {
        init_multithreaded_locked_object(vtdwc,_host_uuid,_log_changes,init_val);
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
        DataWrapper<T, D> to_write_on = acquire_write_lock(active_event);
        to_write_on.write((T)new_val,true);		
    }

	
    @Override
    public boolean return_internal_val_from_container() 
    {
        return true;
    }
	
	
}
