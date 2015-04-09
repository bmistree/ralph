package RalphConnObj;
import java.io.IOException;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public interface ConnectionObj
{
    public void register_host(String host_uuid);
	    
    public void write(GeneralMessage msg_to_write);

    public void close();
}
