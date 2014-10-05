package ralph;

import ralph.Variables.AtomicListVariable;
import ralph.Variables.NonAtomicListVariable;

public interface IListVariableFactory
{
    public AtomicListVariable construct_atomic(RalphGlobals ralph_globals);
    public NonAtomicListVariable construct_non_atomic(
        RalphGlobals ralph_globals);
}
