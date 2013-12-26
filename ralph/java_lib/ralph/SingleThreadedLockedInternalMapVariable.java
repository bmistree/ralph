package ralph;

import java.util.HashMap;

import RalphExceptions.BackoutException;

public class SingleThreadedLockedInternalMapVariable<K,V,D>
    extends SingleThreadedLockedContainer<K,V,D>
{
    private EnsureLockedWrapper<V,D>locked_wrapper;
    public SingleThreadedLockedInternalMapVariable(
        String _host_uuid,boolean _peered,HashMap<K,LockedObject<V,D>> init_val,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        init(_host_uuid,_peered,rtdwc,init_val,index_type);
        _locked_wrapper = locked_wrapper;
    }
    public SingleThreadedLockedInternalMapVariable(
        String _host_uuid,boolean _peered,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        HashMap<K,LockedObject<V,D>> init_val =
            new HashMap<K,LockedObject<V,D>>();
        init(_host_uuid,_peered,rtdwc,init_val,index_type);
        locked_wrapper = _locked_wrapper;
    }
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);		
    }
	
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, K key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        LockedObject<V,D> wrapped_to_write =
            locked_wrapper.ensure_locked_object(to_write);
        set_val_on_key(active_event,key,wrapped_to_write,false);
    }	
}
