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
            new LiveMessageSender(), ralph_globals,
            gen_dur_ctx(act_evt.uuid,ralph_globals),
            act_evt, exec_ctx_map, ralph_globals, ralph_globals.all_endpoints,
            false);
    }

    private static IDurabilityContext gen_dur_ctx(
        String evt_uuid,RalphGlobals ralph_globals)
    {
        if (DurabilityInfo.instance.durability_saver != null)
            return new DurabilityContext(evt_uuid,ralph_globals);
        return null;
    }

    public LiveMessageSender live_message_sender()
    {
        return (LiveMessageSender) message_sender();
    }
}