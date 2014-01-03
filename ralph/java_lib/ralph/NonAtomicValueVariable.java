package ralph;
import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;

public abstract class NonAtomicValueVariable<T,D> extends NonAtomicObject<T,D>
{
    public NonAtomicValueVariable()
    {
        super();
    }

    public NonAtomicValueVariable(
        String _host_uuid, T init_val, T DEFAULT_VALUE,
        ValueTypeDataWrapperFactory<T,D> vtdwc)
    {
        if (init_val == null)
            init_val = DEFAULT_VALUE;
        init(vtdwc,_host_uuid,init_val);
    }
    
    public void init_non_atomic_value_variable(
        String _host_uuid, T init_val, T DEFAULT_VALUE,
        ValueTypeDataWrapperFactory<T,D> vtdwc)
    {
        if (init_val == null)
            init_val = DEFAULT_VALUE;
        init(vtdwc,_host_uuid,init_val);
    }

    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        this.set_val(active_event,(T)to_swap_with.get_val(active_event));
    }
    
	
    public void write_if_different(ActiveEvent active_event,T data)
    {
        val.write(data,true);
    }
	
    public boolean return_internal_val_from_container()
    {
        return true;
    }
}
