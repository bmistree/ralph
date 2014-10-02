package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphVersions.IReconstructionContext;
import RalphVersions.ObjectHistory;

public abstract class AtomicReferenceVariable<ValueType extends IReference>
    extends AtomicVariable<ValueType, IReference>
    implements IInternalReferenceHolder
{
    /**
       When we are replaying reference variables, we first must
       construct them.  Then we replay what they were pointing to.
       This field should hold the name of the reference that this
       object was pointing to when it was constructed.  
     */
    private String initial_reference = null;
    
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

    /*** IInitialReferenceHolder methods */
    
    @Override
    public String get_initial_reference()
    {
        return initial_reference;
    }

    @Override
    public void set_initial_reference(String new_initial_reference)
    {
        initial_reference = new_initial_reference;
    }

    /***** AtomicVariable methods */
    
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
        if ((VersioningInfo.instance.version_saver == null) ||
            (version_helper == null))
        {
            return;
        }
        version_helper.save_version(
            uuid,dirty_val.val,active_event.commit_metadata);
    }

    @Override
    public void replay (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until)
    {
        String reference_to_use =
            ObjectHistory.find_reference(obj_history,to_play_until);

        if (reference_to_use == null)
        {
            if (initial_reference == null)
            {
                Util.logger_assert(
                    "Require a reference to replay from");
            }
            reference_to_use = initial_reference;
        }
        ValueType rebuilt_internal_val =
            (ValueType) reconstruction_context.get_constructed_object(
                reference_to_use, to_play_until);
        direct_set_val(rebuilt_internal_val);
    }
}
