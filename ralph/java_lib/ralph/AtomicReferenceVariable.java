package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;

public abstract class AtomicReferenceVariable<ValueType extends IReference>
    extends AtomicVariable<ValueType, IReference>
{
    /**
       When we are replaying reference variables, we first must
       construct them.  Then we replay what they were pointing to.
       This field holds what the reference was pointing to.
     */
    private String ref_to_replay_from = null;
    
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

    public String get_ref_to_replay_from()
    {
        return ref_to_replay_from;
    }

    public void set_ref_to_replay_from(String new_ref_to_replay_from)
    {
        ref_to_replay_from = new_ref_to_replay_from;
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
