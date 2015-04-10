package ralph.Connection;

import java.util.Set;
import java.util.HashSet;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public abstract class ConnectionListenerManager
{
    private Set<IMessageListener> subscribed_listeners =
        new HashSet<IMessageListener>();

    public synchronized void subscribe_listener(IMessageListener msg_listener)
    {
        subscribed_listeners.add(msg_listener);
    }
    protected synchronized void recvd_msg_to_listeners(GeneralMessage msg)
    {
        for (IMessageListener msg_listener : subscribed_listeners)
            msg_listener.msg_recvd(msg);
    }
}