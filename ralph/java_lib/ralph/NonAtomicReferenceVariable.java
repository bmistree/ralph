package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphVersions.IReconstructionContext;
import RalphVersions.ObjectHistory;

// FIXME: This class is highly-redundant with AtomicReferenceVariable.
// Think about parameterizing based on NonAtomicVariable to get rid of
// extra code.

public abstract class NonAtomicReferenceVariable<ValueType extends IReference>
    extends NonAtomicVariable<ValueType, IReference>
    implements IInternalReferenceHolder
{
    /**
       When we are replaying reference variables, we first must
       construct them.  Then we replay what they were pointing to.
       This field should hold the name of the reference that this
       object was pointing to when it was constructed.  
     */
    private String initial_reference = null;
    private boolean initial_reference_set = false;
    
    public NonAtomicReferenceVariable(
        ValueType init_val, ValueTypeDataWrapperFactory<ValueType> vtdwc,
        VersionHelper<IReference> version_helper,
        RalphGlobals ralph_globals,Object additional_serialization_contents)
    {
        super(
            init_val,vtdwc,version_helper,ralph_globals,
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

    
    /***** NonAtomicVariable methods */
    
    @Override
    public boolean return_internal_val_from_container() 
    {
        return false;
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
            if(! get_initial_reference_set())
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
