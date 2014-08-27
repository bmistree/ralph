package ralph;

public class CommitMetadata
{
    final public long root_commit_lamport_time;
    final public String root_application_uuid;
    final public String event_name;
    final public String event_uuid;

    public CommitMetadata(
        long _root_commit_lamport_time,
        String _root_application_uuid, String _event_name,
        String _event_uuid)
    {
        root_commit_lamport_time = _root_commit_lamport_time;
        root_application_uuid = _root_application_uuid;
        event_name = _event_name;
        event_uuid = _event_uuid;
    }
}