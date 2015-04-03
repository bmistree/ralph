package ralph.MessageManager;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public interface IConnection
{
    public String remote_host_uuid();
    public void subscribe_listener(IMessageListener msg_listener);
    public void send_msg(GeneralMessage msg);
}