package RalphDataWrappers;

import ralph.InternalCodePack;


public class CodePackTypeDataWrapperFactory
    extends ValueTypeDataWrapperFactory<InternalCodePack,InternalCodePack>
{	
    @Override
    public DataWrapper<InternalCodePack, InternalCodePack> construct(
        InternalCodePack _val, boolean log_changes)
    {
        return new ValueTypeDataWrapper<InternalCodePack,InternalCodePack>(
            _val,log_changes);
    }
}
