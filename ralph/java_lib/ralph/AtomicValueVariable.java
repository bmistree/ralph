package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;

public abstract class AtomicValueVariable<T>
    extends AtomicVariable<T,T>
{
    public AtomicValueVariable(
        boolean _log_changes, T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper,
        RalphGlobals ralph_globals)
    {
        this(_log_changes,init_val,vtdwc,version_helper,ralph_globals,null);
    }

    public AtomicValueVariable(
        boolean _log_changes, T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper,
        RalphGlobals ralph_globals,
        Object additional_serialization_contents)
    {
        super(
            _log_changes,init_val,vtdwc,version_helper,ralph_globals,
            additional_serialization_contents);
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
        if ((VersioningInfo.instance.version_saver == null) ||
            (version_helper == null))
        {
            return;
        }
        version_helper.save_version(
            uuid,dirty_val.val,active_event.commit_metadata);
    }
}