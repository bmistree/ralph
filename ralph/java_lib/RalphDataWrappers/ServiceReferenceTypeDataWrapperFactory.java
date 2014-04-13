package RalphDataWrappers;

import ralph.InternalServiceReference;


public class ServiceReferenceTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<InternalServiceReference,InternalServiceReference>
{	
    @Override
    public DataWrapper<InternalServiceReference, InternalServiceReference> construct(
        InternalServiceReference _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<InternalServiceReference,InternalServiceReference>(
            _val,log_changes);
    }
}
