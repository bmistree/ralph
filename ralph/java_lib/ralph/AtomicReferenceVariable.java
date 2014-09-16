package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;

public abstract class AtomicReferenceVariable<ValueType extends IReference>
    extends AtomicVariable<ValueType, IReference>
{
    public AtomicReferenceVariable(
        boolean _log_changes, ValueType init_val,
        ValueTypeDataWrapperFactory<ValueType> vtdwc,
        VersionHelper<IReference> version_helper,
        RalphGlobals ralph_globals,Object additional_serialization_contents)
    {
        super(
            _log_changes,init_val,vtdwc,version_helper,ralph_globals,
            additional_serialization_contents);
    }
    
    @Override
    public boolean return_internal_val_from_container() 
    {
        return false;
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
        if ((VersioningInfo.instance.local_version_manager == null) ||
            (version_helper == null))
        {
            return;
        }
        version_helper.save_version(
            uuid,dirty_val.val,active_event.commit_metadata);
    }
}
