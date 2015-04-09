package RalphConnObj;

import ralph.Endpoint;
import ralph.Util;
import ralph.RalphGlobals;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ArrayBlockingQueue;

import com.google.protobuf.InvalidProtocolBufferException;

public class SameHostConnection implements ConnectionObj
{
    private final RalphGlobals ralph_globals;
    public SameHostConnection(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
    }

    @Override
    public void register_host(String host_uuid)
    {
        // nothing to do here: everything is on same host.
    }
    
    @Override
    public void write(GeneralMessage msg_to_write) 
    {
        // receive the message on same host.
        ralph_globals.message_manager.msg_recvd(msg_to_write);
    }

    @Override
    public void close()
    {
        // nothing to do here, everything is on same host.
    }
}