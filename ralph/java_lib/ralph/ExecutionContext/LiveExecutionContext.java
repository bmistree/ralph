package ralph.ExecutionContext;

import ralph.MessageSender.LiveMessageSender;
import ralph.RalphGlobals;

public class LiveExecutionContext extends ExecutionContext
{
    public LiveExecutionContext(RalphGlobals ralph_globals)
    {
        super(new LiveMessageSender(), ralph_globals);
    }
}