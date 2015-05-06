package ralph.MessageManager;

import java.util.Set;
import java.util.HashSet;

import ralph.Connection.IMessageListener;
import ralph.Connection.ConnectionListenerManager;
import ralph.RalphGlobals;
import ralph.Endpoint;
import ralph.Util;

import RalphServiceActions.ServiceAction;
import RalphServiceActions.ReceiveFirstPhaseCommitMessage;
import RalphServiceActions.ReceiveFirstPhaseCommitMessage;
import RalphServiceActions.ReceivePromotionAction;
import RalphServiceActions.ReceiveRequestBackoutAction;
import RalphServiceActions.ReceiveRequestCompleteCommitAction;
import RalphServiceActions.ReceivePartnerMessageRequestSequenceBlockAction;
import RalphServiceActions.ReceiveRequestCommitAction;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.PartnerFirstPhaseResultMessageProto.PartnerFirstPhaseResultMessage;


/**
   Handles all non-install messages.
 */
public class MessageHandler extends InstallMessageProvider
    implements IMessageListener
{
    final private RalphGlobals ralph_globals;

    public MessageHandler(
        ConnectionListenerManager conn_listener_manager,
        RalphGlobals ralph_globals)
    {
        conn_listener_manager.subscribe_listener(this);
        this.ralph_globals = ralph_globals;
    }

    @Override
    public void msg_recvd(GeneralMessage general_msg)
    {
        // update timestamp.
        String remote_host_uuid = general_msg.getSenderHostUuid().getData();
        long tstamp = general_msg.getTimestamp();
        ralph_globals.clock.check_update_timestamp(tstamp);


        if (general_msg.hasInstall())
        {
            push_install_msg(
               remote_host_uuid, general_msg.getInstall());
        }
        else if (general_msg.hasRequestSequenceBlock())
        {
            ServiceAction service_action =
                new ReceivePartnerMessageRequestSequenceBlockAction(
                    ralph_globals, general_msg.getRequestSequenceBlock(),
                    remote_host_uuid);

            ralph_globals.thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasFirstPhaseResult())
        {
            PartnerFirstPhaseResultMessage fpr = general_msg.getFirstPhaseResult();
            String event_uuid = fpr.getEventUuid().getData();
            String result_initiator_host_uuid = fpr.getSendingHostUuid().getData();

            if (general_msg.getFirstPhaseResult().getSuccessful())
            {
                Set<String> children_event_host_uuids = new HashSet<String>();
                for (int i = 0; i < fpr.getChildrenEventHostUuidsCount(); ++i)
                {
                    String child_event_uuid = fpr.getChildrenEventHostUuids(i).getData();
                    children_event_host_uuids.add(child_event_uuid);
                }

                receive_first_phase_commit_successful(
                    event_uuid,result_initiator_host_uuid,
                    children_event_host_uuids);
            }
            else
            {
                receive_first_phase_commit_unsuccessful(
                    event_uuid,result_initiator_host_uuid);
            }
        }
        else if (general_msg.hasPromotion())
        {
            String event_uuid =
                general_msg.getPromotion().getEventUuid().getData();
            String new_priority =
                general_msg.getPromotion().getNewPriority().getData();
            receive_promotion(event_uuid,new_priority);
        }
        else if (general_msg.hasBackoutCommitRequest())
        {
            String event_uuid =
                general_msg.getBackoutCommitRequest().getEventUuid().getData();
            receive_request_backout(event_uuid);
        }
        else if (general_msg.hasCompleteCommitRequest())
        {
            ServiceAction service_action =
                new ReceiveRequestCompleteCommitAction(
                    ralph_globals, general_msg.getCompleteCommitRequest().getEventUuid().getData(),
                    true);
            ralph_globals.thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasCommitRequest())
        {
            String event_uuid =
                general_msg.getCommitRequest().getEventUuid().getData();
            String root_host_uuid =
                general_msg.getCommitRequest().getRootHostUuid().getData();
            long root_timestamp =
                general_msg.getCommitRequest().getRootTimestamp();

            String application_uuid =
                general_msg.getCommitRequest().getApplicationUuid().getData();
            String event_name =
                general_msg.getCommitRequest().getEventName();

            ServiceAction service_action =
                new ReceiveRequestCommitAction(
                    ralph_globals, event_uuid, root_timestamp, root_host_uuid,
                    application_uuid, event_name);

            ralph_globals.thread_pool.add_service_action(service_action);
        }
        else if (general_msg.hasError())
        {
            Util.logger_warn("Not handling error message.");
        }
        else if (general_msg.hasHeartbeat())
        {
            Util.logger_warn("Not handling heartbeat");
        }
        //#### DEBUG
        else
        {
            Util.logger_assert(
                "Do not know how to convert message to event action " +
                "in _receive_msg_from_partner.");
        }
        //#### END DEBUG
    }


    /**
       One of the endpoints, with uuid host_uuid, that we are
       subscribed to was able to complete first phase commit for
       event with uuid event_uuid.

       @param {uuid} event_uuid --- The uuid of the event associated
       with this message.  (Used to index into local endpoint's
       active event map.)

       @param {uuid} host_uuid --- The uuid of the endpoint that
       was able to complete the first phase of the commit.  (Note:
       this may not be the same uuid as that for the endpoint that
       called _receive_first_phase_commit_successful on this
       endpoint.  We only keep track of the endpoint that originally
       committed.)

       @param {None or list} children_event_host_uuids --- None
       if successful is False.  Otherwise, a set of uuids.  The root
       endpoint should not transition from being in first phase of
       commit to completing commit until it has received a first
       phase successful message from endpoints with each of these
       uuids.

       Forward the message on to the root.
    */
    private void receive_first_phase_commit_successful(
        String event_uuid, String host_uuid,
        Set<String> children_event_host_uuids)
    {
        ServiceAction service_action =
            new ReceiveFirstPhaseCommitMessage(
                ralph_globals, event_uuid, host_uuid, true,
                children_event_host_uuids);

        ralph_globals.thread_pool.add_service_action(service_action);
    }

    /**
       @param {uuid} event_uuid --- The uuid of the event associated
       with this message.  (Used to index into local endpoint's
       active event map.)

       @param {uuid} host_uuid --- The endpoint
       that tried to perform the first phase of the commit.  (Other
       endpoints may have forwarded the result on to us.)
    */
    private void receive_first_phase_commit_unsuccessful(
        String event_uuid, String host_uuid)
    {
    	ServiceAction service_action =
            new ReceiveFirstPhaseCommitMessage(
                ralph_globals, event_uuid, host_uuid, false, null);

        ralph_globals.thread_pool.add_service_action(service_action);
    }

    private void receive_promotion(String event_uuid, String new_priority)
    {
        ServiceAction promotion_action =
            new ReceivePromotionAction(
                ralph_globals, event_uuid, new_priority);
        ralph_globals.thread_pool.add_service_action(promotion_action);
    }

    /**
       @param {uuid} evt_uuid --- The uuid of the _ActiveEvent that we
       want to backout.
    */
    private void receive_request_backout(String evt_uuid)
    {
        ServiceAction req_backout_action =
            new RalphServiceActions.ReceiveRequestBackoutAction(
                ralph_globals, evt_uuid, true);
        ralph_globals.thread_pool.add_service_action(req_backout_action);
    }
}