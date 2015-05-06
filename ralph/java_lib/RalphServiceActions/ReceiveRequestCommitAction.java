package RalphServiceActions;

import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.ExecutionContext.ExecutionContext;

/**
 * The local endpoint's partner has requested the local endpoint to
 begin the first phase of committing changes (note: not complete
 committing the changes, just begin the first phase) for an event.
 *
 */
public class ReceiveRequestCommitAction extends ServiceAction
{
    private final RalphGlobals ralph_globals;
    private final String event_uuid;
    private final Long root_timestamp;
    private final String root_host_uuid;
    private final String application_uuid;
    private final String event_name;

    public ReceiveRequestCommitAction(
        RalphGlobals ralph_globals, String event_uuid,
        Long root_timestamp, String root_host_uuid, String application_uuid,
        String event_name)
    {
        this.ralph_globals = ralph_globals;
        this.event_uuid = event_uuid;
        this.root_timestamp = root_timestamp;
        this.root_host_uuid = root_host_uuid;
        this.application_uuid = application_uuid;
        this.event_name = event_name;
    }

    @Override
    public void run()
    {
        ExecutionContext exec_ctx = ralph_globals.all_ctx_map.get(event_uuid);
        if (exec_ctx == null)
        {
            // can happen if commit is requested and then
            //  a ---> b ---> c
            //
            //     a asks for commit.  b backs out and forwards commit
            //     request on to c.  c waits on active event map lock
            //     before receiving request for commit.  a tells b to back
            //     out and forwards the request to b to backout, which
            //     forwards the request on to c.  Then, if c reads the
            //     backout before the request to commit, we may get to this
            //     point.  Just ignore the request.
        }
        else
        {
            ActiveEvent evt = exec_ctx.curr_act_evt();
            evt.non_local_root_begin_first_phase_commit(
                root_timestamp, root_host_uuid, application_uuid,
                event_name);
        }
    }
}

