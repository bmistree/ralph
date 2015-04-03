package ralph.MessageManager;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public interface IMessageListener
{
    public void msg_recvd(GeneralMessage msg, String sender_host_uuid);
}