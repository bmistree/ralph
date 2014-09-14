package ralph;

import java.io.Serializable;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.DataWrapper;

public abstract class AtomicVariable<T,DeltaType>
    extends SpeculativeAtomicObject<T,DeltaType> 
{
    public AtomicVariable(
        boolean _log_changes, T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<DeltaType> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_multithreaded_locked_object(
            vtdwc,version_helper,_log_changes,init_val);
    }

    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        this.set_val(active_event,(T)to_swap_with.get_val(active_event));
    }
    
    @Override
    public boolean return_internal_val_from_container() 
    {
        return true;
    }
}
