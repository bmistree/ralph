package RalphServiceActions;

import java.util.Set;

import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.Endpoint;
import ralph.ExecutionContext.ExecutionContext;

/**
 * @see ralphEndpoint._EndpointServiceThread.receive_first_phase_commit_message
 *
 */
public class ReceiveFirstPhaseCommitMessage extends ServiceAction
{
    private final RalphGlobals ralph_globals;
    private final String event_uuid;
    private final String msg_originator_host_uuid;
    private final boolean successful;
    private final Set<String> children_event_host_uuids;

    public ReceiveFirstPhaseCommitMessage (
        RalphGlobals ralph_globals, String event_uuid,
        String msg_originator_host_uuid, boolean successful,
        Set<String> children_event_host_uuids)
    {
        this.ralph_globals = ralph_globals;
        this.event_uuid = event_uuid;
        this.msg_originator_host_uuid = msg_originator_host_uuid;
        this.successful = successful;
        this.children_event_host_uuids = children_event_host_uuids;
    }

    @Override
    public void run()
    {
        ExecutionContext exec_ctx = ralph_globals.all_ctx_map.get(event_uuid);

        if (exec_ctx != null)
        {
            ActiveEvent evt = exec_ctx.curr_act_evt();

            if (successful)
            {
                evt.receive_successful_first_phase_commit_msg(
                    event_uuid, msg_originator_host_uuid,
                    children_event_host_uuids);
            }
            else
            {
                evt.receive_unsuccessful_first_phase_commit_msg(
                    event_uuid,msg_originator_host_uuid);
            }
        }
    }
}
