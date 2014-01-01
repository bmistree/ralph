package ralph;

import java.util.ArrayList;
import RalphExceptions.BackoutException;
import RalphAtomicWrappers.EnsureAtomicWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;

public class AtomicInternalListVariable<V,D>
    extends AtomicListContainer<V,D>
{
    public AtomicInternalListVariable(
        String _host_uuid,boolean _peered,ArrayList<RalphObject<V,D>> init_val,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        super();
        ListTypeDataWrapperFactory<V,D>rtdwc =
            new ListTypeDataWrapperFactory<V,D>();
        init_multithreaded_list_container(
            _host_uuid,_peered,rtdwc,init_val,
            _locked_wrapper);
    }
    public AtomicInternalListVariable(
        String _host_uuid,boolean _peered,
        EnsureAtomicWrapper<V,D>_locked_wrapper)
    {
        super();
        ListTypeDataWrapperFactory<V,D>rtdwc =
            new ListTypeDataWrapperFactory<V,D>();
        ArrayList<RalphObject<V,D>> init_val =
            new ArrayList<RalphObject<V,D>>();
        init_multithreaded_list_container(
            _host_uuid,_peered,rtdwc,init_val,
            _locked_wrapper);
    }

    @Override
    public void insert(
        ActiveEvent active_event, Integer index_to_insert_in, V what_to_insert)
        throws BackoutException
    {
        RalphObject<V,D> wrapped_to_write =
            locked_wrapper.ensure_atomic_object(what_to_insert);
        insert(
            active_event, index_to_insert_in, wrapped_to_write);
    }

}
