package ralph.MessageManager;

import java.util.Set;

import ralph.Util;
import ralph.RalphGlobals;
import ralph.Connection.IMessageListener;
import ralph.Connection.IConnection;
import ralph.Connection.ConnectionListenerManager;

import ralph_protobuffs.PromotionProto.Promotion;
import ralph_protobuffs.UtilProto;
import ralph_protobuffs.UtilProto.UUID;
import ralph_protobuffs.InstallProto.Install;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import ralph_protobuffs.PartnerBackoutCommitRequestProto.PartnerBackoutCommitRequest;
import ralph_protobuffs.PartnerCommitRequestProto.PartnerCommitRequest;
import ralph_protobuffs.PartnerCompleteCommitRequestProto.PartnerCompleteCommitRequest;
import ralph_protobuffs.PartnerErrorProto.PartnerError;
import ralph_protobuffs.PartnerFirstPhaseResultMessageProto.PartnerFirstPhaseResultMessage;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.UtilProto.Timestamp;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock.Arguments;
import ralph_protobuffs.DeltaProto.Delta.ServiceFactoryDelta;
import ralph_protobuffs.DeltaProto.Delta.ServiceReferenceDelta;

public class MessageManager extends ConnectionListenerManager
    implements IMessageListener
{
    // contains local endpoints
    private final ConnectionMap conn_map = new ConnectionMap();
    private final RalphGlobals ralph_globals;

    public final InstallMessageProvider install_message_provider;

    public MessageManager(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
        // MessageHandler's constructor will subscribe it as a message
        // listener.
        this.install_message_provider =
            new MessageHandler(this, ralph_globals);
    }

    public void add_connection(IConnection conn)
    {
        conn_map.add_connection(conn);
        conn.subscribe_listener(this);
    }

    @Override
    public void msg_recvd(GeneralMessage msg)
    {
        recvd_msg_to_listeners(msg);
    }

    public Set<String> remote_connection_uuids()
    {
        return conn_map.remote_connection_uuids();
    }

    /**
       Constructs the skeleton of a general message: holds just
       timestamp and sender_host_uuid.
     */
    private GeneralMessage.Builder base_general_msg()
    {
        GeneralMessage.Builder general_message = GeneralMessage.newBuilder();
        UUID.Builder host_uuid_builder = UUID.newBuilder();
        host_uuid_builder.setData(ralph_globals.host_uuid);
        general_message.setSenderHostUuid(host_uuid_builder);
        general_message.setTimestamp(ralph_globals.clock.get_int_timestamp());

        return general_message;
    }

    public void send_install_request(
        String remote_host_uuid, String request_uuid,
        ServiceFactoryDelta service_factory_contents)
    {
        // generate request of install
        Install.Request.Builder req_builder = Install.Request.newBuilder();
        req_builder.setServiceFactory(service_factory_contents);

        // generate install message
        Install.Builder install_msg = Install.newBuilder();
        install_msg.setRequest(req_builder);
        install_msg.setInstallUuid(request_uuid);

        // generate genereal message to wrap install
        GeneralMessage.Builder general_message = base_general_msg();
        general_message.setInstall(install_msg);
        send_msg(remote_host_uuid, general_message.build());
    }

    public void send_install_reply(
        String remote_host_uuid, String request_uuid,
        ServiceReferenceDelta service_reference_contents)
    {
        // generate reply of install
        Install.Reply.Builder reply_builder = Install.Reply.newBuilder();
        reply_builder.setServiceReference(service_reference_contents);

        // generate install message
        Install.Builder install_msg = Install.newBuilder();
        install_msg.setReply(reply_builder);
        install_msg.setInstallUuid(request_uuid);

        // generate genereal message to wrap install
        GeneralMessage.Builder general_message = base_general_msg();
        general_message.setInstall(install_msg);
        send_msg(remote_host_uuid, general_message.build());
    }

    /**
     @param {uuid} event_uuid

     @param {uuid} host_uuid

     @param {array} children_event_host_uuids ---

     @param on_behalf_of Hosts can forward other hosts' first phase
     commit messages to the root of the spanning tree for them. This
     is the uuid of the host that sent the initial message.


     Partner endpoint is subscriber of event on this endpoint with
     uuid event_uuid.  Send to partner a message that the first
     phase of the commit was successful for the endpoint with uuid
     host_uuid, and that the root can go on to second phase of
     commit when all endpoints with uuids in
     children_event_host_uuids have confirmed that they are
     clear to commit.
    */
    public void send_first_phase_commit_successful(
        String remote_host_uuid, String event_uuid,
        Set<String> children_event_host_uuids, String on_behalf_of)
    {
        GeneralMessage.Builder general_message = base_general_msg();

        // construct message to send
        PartnerFirstPhaseResultMessage.Builder first_phase_result_msg =
            PartnerFirstPhaseResultMessage.newBuilder();

        UtilProto.UUID.Builder event_uuid_msg =
            UtilProto.UUID.newBuilder();
        event_uuid_msg.setData(event_uuid);

        UtilProto.UUID.Builder sending_host_uuid_msg =
            UtilProto.UUID.newBuilder();
        sending_host_uuid_msg.setData(on_behalf_of);

        first_phase_result_msg.setSuccessful(true);
        first_phase_result_msg.setEventUuid(event_uuid_msg);
        first_phase_result_msg.setSendingHostUuid(
            sending_host_uuid_msg);

        for (String child_event_uuid : children_event_host_uuids)
        {
            UtilProto.UUID.Builder child_event_uuid_msg =
                UtilProto.UUID.newBuilder();
            child_event_uuid_msg.setData(child_event_uuid);
            first_phase_result_msg.addChildrenEventHostUuids(
                child_event_uuid_msg);
        }

        general_message.setFirstPhaseResult(first_phase_result_msg);

        // actually send the message
        send_msg(remote_host_uuid, general_message.build());
    }

    public void send_backout_request(
        Set<String> remote_host_uuid_set, String event_uuid)
    {
        if (remote_host_uuid_set.size() == 0)
            return;

        GeneralMessage.Builder general_message = base_general_msg();
    	PartnerBackoutCommitRequest.Builder backout_commit_request =
            PartnerBackoutCommitRequest.newBuilder();
    	UtilProto.UUID.Builder event_uuid_builder = UtilProto.UUID.newBuilder();
    	event_uuid_builder.setData(event_uuid);
        backout_commit_request.setEventUuid(event_uuid_builder);
    	general_message.setBackoutCommitRequest(backout_commit_request);

        GeneralMessage msg = general_message.build();

        for (String remote_host_uuid : remote_host_uuid_set)
            send_msg(remote_host_uuid, msg);
    }

    /**
     *  Called by the active event when an exception has occured in
     the midst of a sequence and it needs to be propagated back
     towards the root of the active event. Sends a partner_error
     message to the partner containing the event and endpoint
     uuids.
    */
    public void send_exception_msg(
        String remote_host_uuid, String event_uuid, String priority,
        Exception exception)
    {
        // Construct message
        GeneralMessage.Builder general_message = base_general_msg();

        PartnerError.Builder error = PartnerError.newBuilder();
        UUID.Builder msg_evt_uuid = UUID.newBuilder();
        msg_evt_uuid.setData(event_uuid);
        UUID.Builder msg_host_uuid = UUID.newBuilder();
        msg_host_uuid.setData(ralph_globals.host_uuid);

        error.setEventUuid(msg_evt_uuid);
        error.setHostUuid(msg_host_uuid);

        if (RalphExceptions.NetworkException.class.isInstance(exception))
        {
            error.setType(PartnerError.ErrorType.NETWORK);
            error.setTrace("Incorrect trace for now");
        }
        else if (RalphExceptions.ApplicationException.class.isInstance(exception))
        {
            error.setType(PartnerError.ErrorType.APPLICATION);
            error.setTrace("Incorrect trace for now");
        }
        else
        {
            error.setType(PartnerError.ErrorType.APPLICATION);
            error.setTrace("Incorrect trace for now");
        }

        // actually send the message
        send_msg(remote_host_uuid, general_message.build());
    }


    public void send_promotion_msgs(
        Set<String> remote_host_uuid_set, String event_uuid,
        String new_priority)
    {
        if (remote_host_uuid_set.size() == 0)
            return;

        GeneralMessage.Builder general_message = base_general_msg();
        Promotion.Builder promotion_message = Promotion.newBuilder();

        UtilProto.UUID.Builder event_uuid_builder = UtilProto.UUID.newBuilder();
        event_uuid_builder.setData(event_uuid);

        UtilProto.Priority.Builder new_priority_builder =
            UtilProto.Priority.newBuilder();
        new_priority_builder.setData(new_priority);

        promotion_message.setEventUuid(event_uuid_builder);
        promotion_message.setNewPriority(new_priority_builder);

        general_message.setPromotion(promotion_message);

        GeneralMessage prom_msg = general_message.build();

        for (String remote_host_uuid : remote_host_uuid_set)
            send_msg(remote_host_uuid, prom_msg);
    }

    public void send_commit_request_msgs(
        Set<String> remote_host_uuid_set,
        String active_event_uuid,long root_timestamp,
        String root_host_uuid,String application_uuid,
        String event_name)
    {
        if (remote_host_uuid_set.size() == 0)
            return;

        //# FIXME: may be a way to piggyback commit with final event in
        //# sequence.
        GeneralMessage.Builder general_message = base_general_msg();

    	PartnerCommitRequest.Builder commit_request_msg =
            PartnerCommitRequest.newBuilder();

        // event uuid
    	UtilProto.UUID.Builder event_uuid_msg = UtilProto.UUID.newBuilder();
    	event_uuid_msg.setData(active_event_uuid);
    	commit_request_msg.setEventUuid(event_uuid_msg);

        // root host uuid
    	UtilProto.UUID.Builder root_host_uuid_msg =
            UtilProto.UUID.newBuilder();
    	root_host_uuid_msg.setData(root_host_uuid);
    	commit_request_msg.setRootHostUuid(root_host_uuid_msg);

        // root timestamp
        commit_request_msg.setRootTimestamp(root_timestamp);

        // application_uuid
        UtilProto.UUID.Builder application_uuid_msg =
            UtilProto.UUID.newBuilder();
        application_uuid_msg.setData(application_uuid);
        commit_request_msg.setApplicationUuid(application_uuid_msg);

        // event_name
        commit_request_msg.setEventName(event_name);

        // actually populate general message
    	general_message.setCommitRequest(commit_request_msg);

        GeneralMessage general_commit_request_msg = general_message.build();
        for (String remote_host_uuid : remote_host_uuid_set)
        {
            send_msg(remote_host_uuid, general_commit_request_msg);
        }
    }

    /**
     Sends a message using connection object to the partner endpoint
     requesting it to perform some message sequence action.
    */
    public void send_sequence_block_request_msg(
        String remote_host_uuid,
        PartnerRequestSequenceBlock request_sequence_block_msg)
    {
        GeneralMessage.Builder general_message = base_general_msg();
    	general_message.setRequestSequenceBlock(request_sequence_block_msg);
        send_msg(remote_host_uuid, general_message.build());
    }

    /**
       Active event uuid on this endpoint has completed its commit and
       it wants you to tell partner endpoint as well to complete its
       commit.
    */
    public void send_complete_commit_request_msg(
        Set<String> remote_host_uuid_set, String active_event_uuid)
    {
        if (remote_host_uuid_set.size() == 0)
            return;
        GeneralMessage.Builder general_message = base_general_msg();

    	PartnerCompleteCommitRequest.Builder complete_commit_request_msg =
            PartnerCompleteCommitRequest.newBuilder();

    	UtilProto.UUID.Builder active_event_uuid_msg =
            UtilProto.UUID.newBuilder();
    	active_event_uuid_msg.setData(active_event_uuid);

    	complete_commit_request_msg.setEventUuid(active_event_uuid_msg);

    	general_message.setCompleteCommitRequest(complete_commit_request_msg);
        GeneralMessage complete_commit_request = general_message.build();

        for (String remote_host_uuid : remote_host_uuid_set)
            send_msg(remote_host_uuid, complete_commit_request);
    }

    public void send_msg(String remote_host_uuid, GeneralMessage to_send)
    {
        IConnection conn = conn_map.get_connection(remote_host_uuid);
        if (conn == null) {
            Util.logger_assert("No connection to send message");
        }
        conn.send_msg(to_send);
    }
}