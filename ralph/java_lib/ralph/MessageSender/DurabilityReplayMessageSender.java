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
        Endpoint endpoint, ActiveEvent active_event,RalphObject result)
        throws NetworkException, ApplicationException, BackoutException
    {
        // do nothing here: don't actually have to send message to
        // other side because other side isn't running.
    }

    @Override
    public RalphObject hide_partner_call(
        Endpoint endpoint, ActiveEvent active_event,
        String func_name, boolean first_msg,List<RalphObject> args,
        RalphObject result)
        throws NetworkException, ApplicationException, BackoutException
    {
        // get next rpc result.
        return replay_context.next_rpc_result(
            endpoint.ralph_globals,active_event);
    }
}