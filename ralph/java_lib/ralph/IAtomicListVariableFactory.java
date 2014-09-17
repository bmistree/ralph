package ralph;

import ralph.Variables.AtomicListVariable;

public interface IAtomicListVariableFactory
{
    public AtomicListVariable construct(RalphGlobals ralph_globals);
}
