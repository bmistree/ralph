package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;

public abstract class StructWrapperBaseClass<InternalType extends InternalStructBaseClass>
    extends AtomicReferenceVariable<InternalType>
{
    public StructWrapperBaseClass(
        boolean _log_changes, InternalType init_val,
        ValueTypeDataWrapperFactory<InternalType> vtdwc,
        VersionHelper<IReference> version_helper,
        RalphGlobals ralph_globals,Object additional_serialization_contents)
    {
        super(
            _log_changes,init_val,vtdwc,version_helper,ralph_globals,
            additional_serialization_contents);
    }
}
