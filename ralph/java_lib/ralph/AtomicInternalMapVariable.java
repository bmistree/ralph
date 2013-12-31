package ralph;

import java.util.HashMap;

import RalphExceptions.BackoutException;

public class AtomicInternalMapVariable<K,V,D>
    extends AtomicMapContainer<K,V,D>
{
    public AtomicInternalMapVariable(
        String _host_uuid,boolean _peered,HashMap<K,LockedObject<V,D>> init_val,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        init_multithreaded_locked_container(
            _host_uuid,_peered,rtdwc,init_val,index_type,
            _locked_wrapper);
    }
    public AtomicInternalMapVariable(
        String _host_uuid,boolean _peered,
        SingleThreadedLockedContainer.IndexType index_type,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        HashMap<K,LockedObject<V,D>> init_val =
            new HashMap<K,LockedObject<V,D>>();
        init_multithreaded_locked_container(
            _host_uuid,_peered,rtdwc,init_val,index_type,
            _locked_wrapper);
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
