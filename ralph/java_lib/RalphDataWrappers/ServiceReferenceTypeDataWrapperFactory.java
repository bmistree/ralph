package RalphDataWrappers;

import ralph.InternalServiceReference;


public class ServiceReferenceTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<InternalServiceReference>
{	
    @Override
    public DataWrapper<InternalServiceReference> construct(
        InternalServiceReference _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<InternalServiceReference>(
            _val,log_changes);
    }
}
