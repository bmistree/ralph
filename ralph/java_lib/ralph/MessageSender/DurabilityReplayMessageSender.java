package ralph.MessageSender;

import java.util.List;

import RalphDurability.DurabilityReplayContext;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;

import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RalphObject;
import ralph.Util;
import ralph.ExecutionContext.ExecutionContext;

public class DurabilityReplayMessageSender implements IMessageSender
{
    private final DurabilityReplayContext replay_context;
    
    public DurabilityReplayMessageSender(
        DurabilityReplayContext replay_context)
    {
        this.replay_context = replay_context;
    }

    @Override
    public void hide_sequence_completed_call(
        ExecutionContext exec_ctx, RalphObject result)
        throws NetworkException, ApplicationException, BackoutException
    {
        // do nothing here: don't actually have to send message to
        // other side because other side isn't running.
    }

    @Override
    public RalphObject hide_partner_call(
        String remote_host_uuid, String target_endpt_uuid,
        ExecutionContext exec_ctx, String func_name, boolean first_msg,
        List<RalphObject> args, RalphObject result)
        throws NetworkException, ApplicationException, BackoutException
    {
        // get next rpc result.
        return replay_context.issue_rpc(exec_ctx.ralph_globals, exec_ctx);
    }
}