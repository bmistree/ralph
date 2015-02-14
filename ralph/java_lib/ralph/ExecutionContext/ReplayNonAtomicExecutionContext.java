package ralph.ExecutionContext;

import ralph_protobuffs.DurabilityProto.Durability;

import RalphDurability.IDurabilityContext;
import RalphDurability.DurabilityContext;

import ralph.MessageSender.DurabilityReplayMessageSender;
import ralph.RalphGlobals;
import ralph.NonAtomicActiveEvent;
import ralph.ExecutionContextMap;
import ralph.DurabilityInfo;
import ralph.IEndpointMap;

public class ReplayNonAtomicExecutionContext extends NonAtomicExecutionContext
{    
    public ReplayNonAtomicExecutionContext(
        RalphGlobals ralph_globals, NonAtomicActiveEvent act_evt,
        ExecutionContextMap exec_ctx_map,
        DurabilityReplayMessageSender msg_sender,
        IEndpointMap endpt_map)
    {
        super(
            msg_sender, ralph_globals, null, act_evt, exec_ctx_map,
            ralph_globals,endpt_map);
    }

    @Override
    public Durability prepare_proto_buf()
    {
        return null;
    }
    
    @Override
    public Durability complete_proto_buf(boolean succeeded)
    {
        return null;
    }
}
