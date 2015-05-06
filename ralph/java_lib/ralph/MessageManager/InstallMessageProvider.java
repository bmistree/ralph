package ralph.MessageManager;

import java.util.Set;
import java.util.HashSet;

import ralph_protobuffs.InstallProto.Install;


/**
   Forwards Install requests and replies to listeners.
 */
public class InstallMessageProvider
{
    private Set<IInstallMessageListener> msg_listener_set =
        new HashSet<IInstallMessageListener>();

    public synchronized void subscribe_install_message_listener(
        IInstallMessageListener inst_msg_listener)
    {
        msg_listener_set.add(inst_msg_listener);
    }

    protected synchronized void push_install_msg(
        String msg_sender_remote_host_uuid, Install install_msg)
    {
        for (IInstallMessageListener msg_listener : msg_listener_set)
        {
            msg_listener.recv_install_msg(
                msg_sender_remote_host_uuid, install_msg);
        }
    }
}
