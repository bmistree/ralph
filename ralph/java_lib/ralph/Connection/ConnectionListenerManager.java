package ralph.Connection;

import java.util.Set;
import java.util.HashSet;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public abstract class ConnectionListenerManager implements IConnection
{
    private Set<IMessageListener> subscribed_listeners =
        new HashSet<IMessageListener>();
    
    @Override
    public abstract String remote_host_uuid();
    @Override
    public abstract void send_msg(GeneralMessage msg);
    @Override
    public abstract void close();

    @Override
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