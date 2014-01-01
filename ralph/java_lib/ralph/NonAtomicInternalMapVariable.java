package ralph;

import RalphAtomicWrappers.EnsureAtomicWrapper;
import java.util.HashMap;
import RalphExceptions.BackoutException;

public class NonAtomicInternalMapVariable<K,V,D>
    extends NonAtomicMapContainer<K,V,D>
{

    public NonAtomicInternalMapVariable(
        String _host_uuid,boolean _peered,HashMap<K,LockedObject<V,D>> init_val,
        NonAtomicMapContainer.IndexType index_type,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        init(_host_uuid,_peered,rtdwc,init_val,index_type,
            _locked_wrapper);
    }
    public NonAtomicInternalMapVariable(
        String _host_uuid,boolean _peered,
        NonAtomicMapContainer.IndexType index_type,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        HashMap<K,LockedObject<V,D>> init_val =
            new HashMap<K,LockedObject<V,D>>();
        init(
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
            locked_wrapper.ensure_atomic_object(to_write);
        set_val_on_key(active_event,key,wrapped_to_write,false);
    }	
}
