package ralph;

public interface IAtomicStructWrapperBaseClassFactory
{
    public StructWrapperBaseClass construct(
        RalphGlobals ralph_globals);
    /**
       Generates an atomic struct that wraps a null internal value.
     */
    public StructWrapperBaseClass construct_null_internal(
        RalphGlobals ralph_globals);
}
