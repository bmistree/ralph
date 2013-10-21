package ralph;

import WaldoExceptions.BackoutException;


public class LockedValueVariable<T,D> extends MultiThreadedLockedObject<T,D> 
{
    public LockedValueVariable(
        String _host_uuid, boolean _peered, T init_val,T default_value,
        ValueTypeDataWrapperConstructor<T,D> vtdwc)
    {
        super();
        init(vtdwc,_host_uuid,_peered,init_val);
    }

	
    @Override
    public void write_if_different(
        LockedActiveEvent active_event,
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
