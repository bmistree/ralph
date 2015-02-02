package ralph.ExecutionContext;

import RalphDurability.DurabilityContext;

import ralph.MessageSender.LiveMessageSender;
import ralph.RalphGlobals;

public class LiveExecutionContext extends ExecutionContext
{
    public LiveExecutionContext(
        RalphGlobals ralph_globals, DurabilityContext durability_context)
    {
        super(new LiveMessageSender(), ralph_globals, durability_context);
    }


    public LiveMessageSender live_message_sender()
    {
        return (LiveMessageSender) message_sender();
    }
}