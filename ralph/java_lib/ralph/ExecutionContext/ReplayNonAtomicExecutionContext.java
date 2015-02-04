package ralph.ExecutionContext;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityContext;

import ralph.MessageSender.DurabilityReplayMessageSender;
import ralph.RalphGlobals;
import ralph.NonAtomicActiveEvent;
import ralph.ExecutionContextMap;
import ralph.DurabilityInfo;


public class ReplayNonAtomicExecutionContext extends NonAtomicExecutionContext
{
    public ReplayNonAtomicExecutionContext(
        RalphGlobals ralph_globals, NonAtomicActiveEvent act_evt,
        ExecutionContextMap exec_ctx_map,
        DurabilityReplayMessageSender msg_sender)
    {
        super(
            msg_sender, ralph_globals, null, act_evt, exec_ctx_map,
            ralph_globals);
    }
}