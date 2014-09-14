package ralph;

import RalphDataWrappers.ValueTypeDataWrapperFactory;

public abstract class NonAtomicValueVariable<T>
    extends NonAtomicVariable<T,T>
{
    public NonAtomicValueVariable(
        T init_val,ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<T> version_helper, RalphGlobals ralph_globals)
    {
        super(init_val,vtdwc,version_helper,ralph_globals);
    }
}