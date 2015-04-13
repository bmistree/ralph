package ralph.Install;

import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import ralph.Connection.IConnection;
import ralph.MessageManager.InstallMessageProvider;
import ralph.MessageManager.IInstallMessageListener;
import ralph.MessageManager.MessageManager;
import ralph.Util;
import ralph.InternalServiceFactory;
import ralph.InternalServiceReference;
import ralph.Variables;
import ralph.RalphGlobals;

import ralph_protobuffs.InstallProto.Install;
import ralph_protobuffs.DeltaProto.Delta;

public class InstallSender implements IInstallMessageListener
{
    private final RalphGlobals ralph_globals;
    private final ConcurrentMap<String, InstallSettableFuture>
        outstanding_request_map =
        new ConcurrentHashMap<String, InstallSettableFuture>();

    
    public InstallSender(
        InstallMessageProvider inst_msg_provider,
        RalphGlobals ralph_globals)
    {
        inst_msg_provider.subscribe_install_message_listener(this);
        this.ralph_globals = ralph_globals;
    }

    public Future<InternalServiceReference> install_remote (
        String remote_uuid, InternalServiceFactory internal_service_factory)
    {
        Delta.ServiceFactoryDelta.Builder service_factory_delta = 
            Variables.AtomicServiceFactoryVariable.internal_service_factory_serialize(
                internal_service_factory);

        InstallSettableFuture settable_future = new InstallSettableFuture();
        String request_uuid = ralph_globals.generate_local_uuid();
        outstanding_request_map.put(request_uuid, settable_future);

        // issue message
        ralph_globals.message_manager.send_install_request(
            remote_uuid, request_uuid, service_factory_delta.build());
        return settable_future;
    }

    
    @Override
    public void recv_install_msg(Install msg)
    {
        if (msg.hasReply())
        {
            String replying_to_uuid = msg.getInstallUuid();
            handle_install_reply (replying_to_uuid, msg.getReply());
        }
        else if (msg.hasRequest())
        {
            handle_install_request();
        }
        //// DEBUG
        else
        {
            Util.logger_assert("Unknown install message type.");
        }
        //// END DEBUG
    }

    protected void handle_install_request()
    {
        // FIXME
        Util.logger_assert("Must finish handle_install_request");
    }
    
    protected void handle_install_reply(
        String replying_to_uuid, Install.Reply reply_msg)
    {
        InstallSettableFuture install_future =
            outstanding_request_map.remove(replying_to_uuid);

        //// DEBUG
        if (install_future == null)
        {
            Util.logger_assert("Response to unknown install request");
        }
        //// END DEBUG

        install_future.handle_install_reply(reply_msg);
    }
}