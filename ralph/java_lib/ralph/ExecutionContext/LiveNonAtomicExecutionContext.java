package ralph.ExecutionContext;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityContext;

import ralph.MessageSender.LiveMessageSender;
import ralph.RalphGlobals;
import ralph.NonAtomicActiveEvent;
import ralph.ExecutionContextMap;
import ralph.DurabilityInfo;

public class LiveNonAtomicExecutionContext extends NonAtomicExecutionContext
{
    public LiveNonAtomicExecutionContext(
        RalphGlobals ralph_globals, NonAtomicActiveEvent act_evt,
        ExecutionContextMap exec_ctx_map)
    {
        super(
            new LiveMessageSender(), ralph_globals, gen_dur_ctx(act_evt.uuid),
            act_evt, exec_ctx_map, ralph_globals);
    }

    private static IDurabilityContext gen_dur_ctx(String evt_uuid)
    {
        if (DurabilityInfo.instance.durability_saver != null)
            return new DurabilityContext(evt_uuid);
        return null;
    }

    public LiveMessageSender live_message_sender()
    {
        return (LiveMessageSender) message_sender();
    }
}