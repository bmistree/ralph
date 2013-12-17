package ralph;

import java.util.HashMap;

import RalphExceptions.BackoutException;

public class SingleThreadedLockedInternalMapVariable<K,V,D>
    extends SingleThreadedLockedContainer<K,V,D>
{
    public SingleThreadedLockedInternalMapVariable(
        String _host_uuid,boolean _peered,HashMap<K,LockedObject<V,D>> init_val,
        SingleThreadedLockedContainer.IndexType index_type)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        init(_host_uuid,_peered,rtdwc,init_val,index_type);
    }
    public SingleThreadedLockedInternalMapVariable(
        String _host_uuid,boolean _peered,
        SingleThreadedLockedContainer.IndexType index_type)
    {
        super();
        ReferenceTypeDataWrapperConstructor<K,V,D>rtdwc =
            new ReferenceTypeDataWrapperConstructor<K,V,D>();
        HashMap<K,LockedObject<V,D>> init_val =
            new HashMap<K,LockedObject<V,D>>();
        init(_host_uuid,_peered,rtdwc,init_val,index_type);
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
            (LockedObject<V,D>)LockedVariables.ensure_locked_obj(to_write,host_uuid,false);
        set_val_on_key(active_event,key,wrapped_to_write,false);
    }	
}
