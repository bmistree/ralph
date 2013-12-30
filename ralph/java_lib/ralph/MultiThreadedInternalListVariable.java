package ralph;

import java.util.ArrayList;
import RalphExceptions.BackoutException;


public class MultiThreadedInternalListVariable<V,D>
    extends MultiThreadedListContainer<V,D>
{
    public MultiThreadedInternalListVariable(
        String _host_uuid,boolean _peered,ArrayList<LockedObject<V,D>> init_val,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        super();
        ListTypeDataWrapperConstructor<V,D>rtdwc =
            new ListTypeDataWrapperConstructor<V,D>();
        init_multithreaded_list_container(
            _host_uuid,_peered,rtdwc,init_val,
            _locked_wrapper);
    }
    public MultiThreadedInternalListVariable(
        String _host_uuid,boolean _peered,
        EnsureLockedWrapper<V,D>_locked_wrapper)
    {
        super();
        ListTypeDataWrapperConstructor<V,D>rtdwc =
            new ListTypeDataWrapperConstructor<V,D>();
        ArrayList<LockedObject<V,D>> init_val =
            new ArrayList<LockedObject<V,D>>();
        init_multithreaded_list_container(
            _host_uuid,_peered,rtdwc,init_val,
            _locked_wrapper);
    }
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key, V to_write) throws BackoutException
    {
        set_val_on_key(active_event,key,to_write,false);		
    }
	
	
    @Override
    public void set_val_on_key(
        ActiveEvent active_event, Integer key,
        V to_write, boolean copy_if_peered) throws BackoutException 
    {
        LockedObject<V,D> wrapped_to_write =
            locked_wrapper.ensure_locked_object(to_write);
        set_val_on_key(active_event,key,wrapped_to_write,false);
    }	
}
