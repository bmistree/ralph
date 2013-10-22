package RalphConnObj;
import java.io.IOException;

import waldo_protobuffs.GeneralMessageProto.GeneralMessage;

public interface ConnectionObj {
	
    public void register_endpoint(waldo.Endpoint endpoint);
	    
    public void  write(GeneralMessage msg_to_write,waldo.Endpoint endpoint_writing);

    public void write_stop(GeneralMessage msg_to_write,waldo.Endpoint endpoint_writing);

    public void close();
	        
}
