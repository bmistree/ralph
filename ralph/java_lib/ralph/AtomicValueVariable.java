package ralph;

import java.io.Serializable;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.DataWrapper;

public abstract class AtomicValueVariable<T>
    extends SpeculativeAtomicObject<T,T> 
{
    public AtomicValueVariable(
        boolean _log_changes, T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_multithreaded_locked_object(
            vtdwc,version_helper,_log_changes,init_val);
    }

    public AtomicValueVariable(RalphGlobals ralph_globals)
    {
        super (ralph_globals);
    }

    public void init_atomic_value_variable(
        boolean _log_changes, T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper)
    {
        init_multithreaded_locked_object(
            vtdwc,version_helper,_log_changes,init_val);
    }
    
    /**
       Log completed commit, if ralph globals designates to.
     */
    @Override
    public void complete_write_commit_log(
        ActiveEvent active_event)
    {
        RalphGlobals ralph_globals = active_event.ralph_globals;
        // do not do anything
        if ((ralph_globals.local_version_manager == null) ||
            (version_helper == null))
        {
            return;
        }
        version_helper.save_version(
            uuid,dirty_val.val,active_event.commit_metadata);
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
