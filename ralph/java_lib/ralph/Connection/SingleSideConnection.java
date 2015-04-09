package ralph.Connection;

import ralph.Endpoint;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

/**
   Should just be a dummy implementation that doesn't send or use any
   messages.
 */
public enum SingleSideConnection implements IConnection
{
    INSTANCE;
        
    private SingleSideConnection()
    {}
    
    @Override
    public String remote_host_uuid()
    {
        return null;
    }
    @Override
    public void subscribe_listener(IMessageListener msg_listener)
    { }
    @Override
    public void send_msg(GeneralMessage msg)
    { }
    @Override
    public void close()
    { }

    
}
