package ralph;

import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphExceptions.BackoutException;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphDataWrappers.ListTypeDataWrapper;
import java.util.ArrayList;

public class NonAtomicInternalListVariable<V,D>
    extends NonAtomicListContainer<V,D>
{
    public NonAtomicInternalListVariable(
        String _host_uuid,boolean _peered,ArrayList<RalphObject<V,D>> init_val,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        super();
        ListTypeDataWrapperFactory<V,D>rtdwc =
            new ListTypeDataWrapperFactory<V,D>();
        init(_host_uuid,_peered,rtdwc,init_val,
            _locked_wrapper);
    }
    public NonAtomicInternalListVariable(
        String _host_uuid,boolean _peered,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        super();
        ListTypeDataWrapperFactory<V,D>rtdwc =
            new ListTypeDataWrapperFactory<V,D>();
        ArrayList<RalphObject<V,D>> init_val =
            new ArrayList<RalphObject<V,D>>();
        init(
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
        RalphObject<V,D> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(to_write);
        set_val_on_key(active_event,key,wrapped_to_write,false);
    }	
}
