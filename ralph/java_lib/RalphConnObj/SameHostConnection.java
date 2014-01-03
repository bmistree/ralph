package RalphConnObj;

import ralph.Endpoint;
import ralph.Util;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.protobuf.InvalidProtocolBufferException;

public class SameHostConnection implements Runnable, ConnectionObj {

    private class SameHostQueueElement
    {
        public byte[] bytes;
        public ralph.Endpoint endpoint;
        public SameHostQueueElement(
            GeneralMessage msg_to_write, ralph.Endpoint _endpoint)
        {
            bytes = msg_to_write.toByteArray();
            endpoint = _endpoint;
        }
    }
	
	
	
    ArrayBlockingQueue<SameHostQueueElement> queue =
        new ArrayBlockingQueue<SameHostQueueElement>(Util.QUEUE_CAPACITIES);
    Endpoint endpoint1 = null;
    Endpoint endpoint2 = null;
    ReentrantLock endpoint_mutex = new ReentrantLock();
	
	
    public SameHostConnection()
    {}
	
    @Override
    public void register_endpoint(ralph.Endpoint endpoint)
    {
        endpoint_mutex.lock();
        if (endpoint1 == null)
            endpoint1 = endpoint;
        else
        {
            // now have both endpoints
            endpoint2 = endpoint;
            endpoint1._set_partner_uuid(endpoint2._uuid);
            endpoint2._set_partner_uuid(endpoint1._uuid);
            Thread to_start = new Thread(this);
            to_start.setDaemon(true);
            to_start.start();
        }
        endpoint_mutex.unlock();
    }
	
    @Override
    public void write(
        GeneralMessage msg_to_write, Endpoint endpoint_writing) 
    {
        //# write same message back to self
        queue.add(
            new SameHostQueueElement(msg_to_write,endpoint_writing));
    }

    @Override
    public void write_stop(
        GeneralMessage msg_to_write, Endpoint endpoint_writing)
    {
        write(msg_to_write,endpoint_writing);
    }

    @Override
    public void close() { }

	
    public void run()
    {
        while (true)
    	{
            SameHostQueueElement q_elem =null;
            try {
                q_elem = queue.take();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
			
        	
            GeneralMessage msg = null;
            try {
                msg = GeneralMessage.parseFrom(q_elem.bytes);
            } catch (InvalidProtocolBufferException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        	
            Endpoint msg_sender_endpt = q_elem.endpoint;
            Endpoint msg_recvr_endpt = endpoint1;
            if (msg_sender_endpt._uuid.equals(endpoint1._uuid))
                msg_recvr_endpt = endpoint2;

            msg_recvr_endpt._receive_msg_from_partner(msg);
    	}
    }
}