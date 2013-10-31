package RalphConnObj;

import ralph.Endpoint;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public class SingleSideConnection implements ConnectionObj {

    private ralph.Endpoint endpoint = null;
	
    @Override
    public void register_endpoint(Endpoint _endpoint) 
    {
        endpoint = _endpoint;
    }

    @Override
    public void write(
        GeneralMessage msg_to_write, Endpoint endpoint_writing)
    {
        return;
    }

    @Override
    public void write_stop(
        GeneralMessage msg_to_write, Endpoint endpoint_writing) 
    {
        endpoint._receive_msg_from_partner(msg_to_write);
    }

    @Override
    public void close() { }

}
