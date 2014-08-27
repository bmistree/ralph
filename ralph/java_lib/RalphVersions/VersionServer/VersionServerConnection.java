package RalphVersions.VersionServer;

import java.net.Socket;

import RalphVersions.IVersionManager;

public class VersionServerConnection
{
    private final Socket version_requester_socket;
    final IVersionManager version_manager;
    public VersionServerConnection(
        Socket _version_requester_socket,IVersionManager _version_manager)
    {
        version_requester_socket = _version_requester_socket;
        version_manager = _version_manager;
    }
}
