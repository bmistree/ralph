package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphVersions.IReconstructionContext;
import RalphVersions.ObjectHistory;
import RalphExceptions.BackoutException;

public abstract class AtomicReferenceVariable<ValueType extends IReference>
    extends AtomicVariable<ValueType, IReference>
    implements IInternalReferenceHolder
{
    /**
       When we are replaying reference variables, we first must
       construct them.  Then we replay what they were pointing to.
       This field should hold the name of the reference that this
       object was pointing to when it was constructed.  Note that
       initial_reference can be set to null (ie., if wrapping internal
       class that is null).
     */
    private String initial_reference = null;
    private boolean initial_reference_set = false;
    
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
        initial_reference_set = true;
    }

    @Override
    public boolean get_initial_reference_set()
    {
        return initial_reference_set;
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

    public static <InternalType> InternalType replay_deserialize_internal_val_helper(
        IInternalReferenceHolder internal_reference_holder,
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until)
    {
        String reference_to_use =
            ObjectHistory.find_reference(obj_history,to_play_until);
        
        if (reference_to_use == null)
        {
            if(! internal_reference_holder.get_initial_reference_set())
            {
                Util.logger_assert(
                    "Require a reference to replay from");
            }
            reference_to_use =
                internal_reference_holder.get_initial_reference();

            // If reference_to_use is set to null, means that internal
            // value should be null and we shouldn't replay any
            // farther.
            if (reference_to_use == null)
                return null;
        }
        
        InternalType rebuilt_internal_val =
            (InternalType) reconstruction_context.get_constructed_object(
                reference_to_use, to_play_until);
        return rebuilt_internal_val;
        
    }
    
    @Override
    public void replay (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until)
    {
        ValueType rebuilt_internal_val =
            AtomicReferenceVariable.<ValueType>replay_deserialize_internal_val_helper(
                this,reconstruction_context,obj_history, to_play_until);
        direct_set_val(rebuilt_internal_val);
    }
    
    @Override
    public void deserialize (
        IReconstructionContext reconstruction_context,
        ObjectHistory obj_history,Long to_play_until,
        ActiveEvent act_event)
        throws BackoutException
    {
        ValueType rebuilt_internal_val =
            AtomicReferenceVariable.<ValueType>replay_deserialize_internal_val_helper(
                this,reconstruction_context,obj_history, to_play_until);
        set_val(act_event,rebuilt_internal_val);
    }
}
