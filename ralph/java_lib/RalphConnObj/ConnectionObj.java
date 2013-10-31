package RalphConnObj;
import java.io.IOException;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public interface ConnectionObj {
	
    public void register_endpoint(ralph.Endpoint endpoint);
	    
    public void  write(
        GeneralMessage msg_to_write,ralph.Endpoint endpoint_writing);

    public void write_stop(
        GeneralMessage msg_to_write,ralph.Endpoint endpoint_writing);

    public void close();
	        
}
