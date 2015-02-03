package ralph.ExecutionContext;

import RalphDurability.IDurabilityContext;

import ralph.MessageSender.LiveMessageSender;
import ralph.RalphGlobals;

public class LiveExecutionContext extends ExecutionContext
{
    public LiveExecutionContext(
        RalphGlobals ralph_globals, IDurabilityContext durability_context)
    {
        super(
            ralph_globals.generate_uuid(),
            new LiveMessageSender(), ralph_globals, durability_context);
    }

    public LiveExecutionContext(
        RalphGlobals ralph_globals, IDurabilityContext durability_context,
        String uuid)
    {
        super(
            uuid,new LiveMessageSender(), ralph_globals, durability_context);
    }
    
    public LiveMessageSender live_message_sender()
    {
        return (LiveMessageSender) message_sender();
    }
}