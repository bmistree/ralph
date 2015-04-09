package RalphConnObj;

import ralph.Endpoint;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

/**
   Should just be a dummy implementation that doesn't send or use any
   messages.
 */
public class SingleSideConnection implements ConnectionObj
{
    @Override
    public void register_host(String host_uuid) 
    {}

    @Override
    public void write(GeneralMessage msg_to_write)
    {}

    @Override
    public void close()
    { }
}
