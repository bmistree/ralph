package ralph.MessageManager;

import java.util.Set;
import java.util.HashSet;

import ralph.Connection.IMessageListener;
import ralph.Connection.ConnectionListenerManager;

import ralph_protobuffs.InstallProto.Install;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;


/**
   Forwards Install requests and replies to listeners.
 */
public class InstallMessageProvider implements IMessageListener
{
    private Set<IInstallMessageListener> msg_listener_set =
        new HashSet<IInstallMessageListener>();

    public InstallMessageProvider(ConnectionListenerManager manager)
    {
        manager.subscribe_listener(this);
    }

    @Override
    public void msg_recvd(GeneralMessage msg)
    {
        if (msg.hasInstall())
            push_install_msg(msg.getInstall());
    }
    
    public synchronized void subscribe_install_message_listener(
        IInstallMessageListener inst_msg_listener)
    {
        msg_listener_set.add(inst_msg_listener);
    }

    protected synchronized void push_install_msg(Install install_msg)
    {
        for (IInstallMessageListener msg_listener : msg_listener_set)
            msg_listener.recv_install_msg(install_msg);
    }
}
