package ralph.Install;

import java.util.Map;
import java.util.Set;

import RalphCallResults.MessageCallResultObject;
import ralph.MVar;
import ralph.EventParent;
import ralph.ActiveEvent;
import ralph.RalphGlobals;

public class InstallEventParent extends EventParent
{
    public InstallEventParent (RalphGlobals ralph_globals)
    {
        super(ralph_globals.generate_local_uuid(), null,
              ralph_globals, true, null, "install", null);
    }
    
    public void put_exception(
        Exception error,
        Map<String, MVar<MessageCallResultObject>> message_listening_mvars_map)
    {}

    public void receive_successful_first_phase_commit_msg(
        ActiveEvent event, String msg_originator_host_uuid,
        Set<String>children_event_host_uuids)
    {}

    public void first_phase_transition_success(
        Set<String> remote_hosts_contacted_uuid, ActiveEvent event,
        long root_commit_timestamp,
        String root_host_uuid, String application_uuid,
        String event_name)
    {}

    public void second_phase_transition_success()
    {}
}