package ralph.Connection;

import ralph.Endpoint;
import ralph.Util;
import ralph.RalphGlobals;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.protobuf.InvalidProtocolBufferException;

/**
   Generally, should only have a single same host connection per
   running Ralph instance.  This is because don't want conflicting
   SameHostConnections with the same remote host uuid (that's the same
   as our own).

   Each RalphGlobals will have its own version of this object.
 */
public class SameHostConnection extends ConnectionListenerManager
{
    private final RalphGlobals ralph_globals;

    /**
       Should only be called constructing RalphGlobals.  See note at
       top of class.
     */
    public SameHostConnection(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
        this.ralph_globals.message_manager.add_connection(this);
    }

    @Override
    public String remote_host_uuid()
    {
        return ralph_globals.host_uuid;
    }
    
    @Override
    public void send_msg(GeneralMessage msg_to_write) 
    {
        // receive the message on same host.
        recvd_msg_to_listeners(msg_to_write);
    }

    @Override
    public void close()
    {
        // nothing to do here, everything is on same host.
    }
}