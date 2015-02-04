package ralph;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityReplayContext;

public interface IAtomicStructWrapperBaseClassFactory
{
    public StructWrapperBaseClass construct(
        RalphGlobals ralph_globals, IDurabilityContext durability_context,
        DurabilityReplayContext durability_replay_context);
    
    /**
       Generates an atomic struct that wraps a null internal value.
     */
    public StructWrapperBaseClass construct_null_internal(
        RalphGlobals ralph_globals);
}
