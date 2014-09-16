package ralph;

import ralph.Variables.AtomicMapVariable;

public interface IAtomicMapVariableFactory
{
    public AtomicMapVariable construct(RalphGlobals ralph_globals);
}
