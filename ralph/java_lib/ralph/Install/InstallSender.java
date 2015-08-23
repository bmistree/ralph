package ralph.Install;

import java.util.concurrent.Future;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.ByteString;

import ralph.Connection.IConnection;
import ralph.MessageManager.InstallMessageProvider;
import ralph.MessageManager.IInstallMessageListener;
import ralph.MessageManager.MessageManager;
import ralph.Util;
import ralph.InternalServiceFactory;
import ralph.InternalServiceReference;
import ralph.Variables;
import ralph.RalphGlobals;
import ralph.Endpoint;

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
    public void recv_install_msg(String from_host_uuid, Install msg)
    {
        String install_uuid = msg.getInstallUuid();
        if (msg.hasReply())
            handle_install_reply (install_uuid, msg.getReply());
        else if (msg.hasRequest())
        {
            handle_install_request(
                from_host_uuid, install_uuid, msg.getRequest());
        }
        //// DEBUG
        else
        {
            Util.logger_assert("Unknown install message type.");
        }
        //// END DEBUG
    }

    protected void handle_install_request(
        String reply_to_remote_host_uuid, String install_uuid,
        Install.Request request_msg)
    {
        // deserialize message
        ByteString byte_string =
            request_msg.getServiceFactory().getSerializedFactory();

        InternalServiceFactory internal_service_factory =
            InternalServiceFactory.deserialize (byte_string, ralph_globals);

        // actually construct the object locally
        InstallActiveEvent install_act_evt =
            new InstallActiveEvent(ralph_globals);
        Endpoint local_endpt =
            internal_service_factory.construct(install_act_evt);

        ralph_globals.initialize_installed(local_endpt);

        // construct and send reply
        InternalServiceReference service_ref =
            new InternalServiceReference(
                ralph_globals.host_uuid, local_endpt._uuid);

        Delta.ServiceReferenceDelta.Builder service_reference_delta =
            Variables.AtomicServiceReferenceVariable.internal_service_reference_serialize(
                service_ref);

        ralph_globals.message_manager.send_install_reply(
            reply_to_remote_host_uuid, install_uuid,
            service_reference_delta.build());
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