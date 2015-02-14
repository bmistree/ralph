package ralph.ExecutionContext;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityContext;

import ralph.MessageSender.IMessageSender;
import ralph.RalphGlobals;
import ralph.AtomicActiveEvent;
import ralph.DurabilityInfo;
import ralph.MessageSender.LiveMessageSender;


public class LiveAtomicExecutionContext extends AtomicExecutionContext
{
    private LiveAtomicExecutionContext(
        RalphGlobals ralph_globals, IDurabilityContext durability_context,
        AtomicActiveEvent act_evt)
    {
        super(
            new LiveMessageSender(), ralph_globals, durability_context,
            act_evt, null, ralph_globals,ralph_globals.all_endpoints);
    }

    public static LiveAtomicExecutionContext exec_ctx_from_non_atomic(
        RalphGlobals ralph_globals, IDurabilityContext durability_context,
        AtomicActiveEvent act_evt)
    {
        return new LiveAtomicExecutionContext(
            ralph_globals,durability_context,act_evt);
    }

    public static LiveAtomicExecutionContext exec_ctx_not_from_non_atomic(
        RalphGlobals ralph_globals, AtomicActiveEvent act_evt)
    {
        IDurabilityContext dur_ctx = null;
        if (DurabilityInfo.instance.durability_saver != null)
            dur_ctx = new DurabilityContext(act_evt.uuid);
            
        return new LiveAtomicExecutionContext(
            ralph_globals,dur_ctx,act_evt);
    }
}
