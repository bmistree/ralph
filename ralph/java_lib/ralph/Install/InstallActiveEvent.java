package ralph.Install;

import ralph.NonAtomicActiveEvent;
import ralph.RalphGlobals;
import ralph.ExecutionContextMap;

public class InstallActiveEvent extends NonAtomicActiveEvent
{
    public InstallActiveEvent(RalphGlobals ralph_globals)
    {
        super( new InstallEventParent(ralph_globals),
               new ExecutionContextMap(ralph_globals, null),
               ralph_globals);
    }
}