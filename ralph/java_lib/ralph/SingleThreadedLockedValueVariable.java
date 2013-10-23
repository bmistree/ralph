package ralph;


public abstract class SingleThreadedLockedValueVariable<T,D> extends SingleThreadedLockedObject<T,D>
{
    public SingleThreadedLockedValueVariable(
        String _host_uuid, boolean _peered,
        T init_val, T DEFAULT_VALUE, ValueTypeDataWrapperConstructor<T,D> vtdwc)
    {
        if (init_val == null)
            init_val = DEFAULT_VALUE;
        init(vtdwc,_host_uuid,_peered,init_val);
    }

	
    public void write_if_different(LockedActiveEvent active_event,T data)
    {
        val.write(data,true);
    }
	
    public boolean return_internal_val_from_container()
    {
        return true;
    }
}
