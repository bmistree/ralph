package ralph;

public abstract class AtomicReferenceVariable<ValueType extends IReference>
    extends AtomicVariable<ValueType, IReference>
{
    public AtomicReferenceVariable(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
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
        if ((ralph_globals.local_version_manager == null) ||
            (version_helper == null))
        {
            return;
        }
        version_helper.save_version(
            uuid,dirty_val.val,active_event.commit_metadata);
    }
}
