package ralph;

import ralph.Variables.AtomicMapVariable;
import ralph.Variables.NonAtomicMapVariable;

public interface IMapVariableFactory
{
    public AtomicMapVariable construct_atomic(RalphGlobals ralph_globals);
    public NonAtomicMapVariable construct_non_atomic(
        RalphGlobals ralph_globals);
}
