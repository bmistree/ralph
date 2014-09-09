package RalphDeserializer;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import RalphCallResults.MessageCallResultObject;
import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import RalphExceptions.StoppedException;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;

import ralph.Util;
import ralph.EventParent;
import ralph.ThreadPool;
import ralph.ActiveEvent;
import ralph.AtomicObject;
import ralph.Endpoint;
import ralph.ExecutingEventContext;
import ralph.RalphObject;


public class DeserializationEvent extends ActiveEvent
{
    // Note that deserialization events are guaranteed to run
    // independently on objects that no other event will run on until
    // deserialization event is complete.  Therefore, assigning
    // priority and uuid to constant values should not matter.
    final static private String dummy_priority = "a";
    final static private String DESERIALIZATION_UUID = "a";
    
    public DeserializationEvent()
    {
        super(DESERIALIZATION_UUID, null,null);
    }
    
    public ActiveEvent clone_atomic() throws StoppedException
    {
        Util.logger_assert(
            "Cannot clone a deserialization event");
        return null;
    }
    
    public ActiveEvent restore_from_atomic()
    {
        Util.logger_assert(
            "Cannot restore_from_atomic a deserialization event");
        return null;
    }
    public boolean add_touched_obj(AtomicObject obj)
    {
        return true;
    }
    public boolean remove_touched_obj(AtomicObject obj)
    {
        return true;
    }
    
    public void promote_boosted(String new_priority)
    {
        Util.logger_assert(
            "Cannot promote_boosted a deserialization event");
    }
    
    public boolean can_backout_and_hold_lock()
    {
        return true;
    }

    public FirstPhaseCommitResponseCode local_root_begin_first_phase_commit()
    {
        return FirstPhaseCommitResponseCode.SUCCEEDED;
    }
    public FirstPhaseCommitResponseCode non_local_root_begin_first_phase_commit(
        Long root_first_phase_commit_timestamp,
        String root_first_phase_commit_host_uuid,
        String application_uuid,String event_name)
    {
        return FirstPhaseCommitResponseCode.SUCCEEDED;
    }
    
    public void second_phase_commit()
    {}

    public void _backout_touched_objs()
    {}
    public void put_exception(Exception error)
    {
        Util.logger_assert(
            "Cannot put_exception into a deserialization event");
    }
    public void stop(boolean skip_partner)
    {}
    public void blocking_backout(
        String backout_requester_host_uuid, boolean stop_request)
    {}
    public void non_blocking_backout(
        String backout_requester_host_uuid, boolean stop_request)
    {}
    public void obj_request_backout_and_release_lock(
        AtomicObject obj_requesting)
    {}
    public void obj_request_no_backout_and_release_lock()
    {}
    public boolean immediate_complete()
    {
        return true;
    }
    public void handle_backout_exception(BackoutException be)
    {
        Util.logger_assert(
            "Should never receive a backout exception in " +
            "deserialization event");
    }

    @Override
    public boolean issue_partner_sequence_block_call(
        Endpoint endpoint, ExecutingEventContext ctx, String func_name,
        ArrayBlockingQueue<MessageCallResultObject>threadsafe_unblock_queue,
        boolean first_msg,List<RalphObject>args,RalphObject result)
    {
        Util.logger_assert(
            "Should never issue_partner_sequence_block_call " +
            "deserialization event");
        return true;
    }

    public String get_priority()
    {
        // can return any priority; there should not be conflicts when
        // deserializing.
        return dummy_priority;
    }

    @Override
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_host_uuid,
        List<String> children_event_host_uuids)
    {}

    public void complete_commit_and_forward_complete_msg(
        boolean request_from_partner)
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }
    public void forward_backout_request_and_backout_self()
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }
    public void forward_backout_request_and_backout_self(
        boolean skip_partner)
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }
    public void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out)
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }

    public void forward_backout_request_and_backout_self(
        boolean skip_partner, boolean already_backed_out,
        boolean stop_request)
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }
    
    public void recv_partner_sequence_call_msg(
        Endpoint endpt_recvd_on,
        PartnerRequestSequenceBlock msg)
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }

    public void receive_unsuccessful_first_phase_commit_msg(
        String event_uuid,
        String msg_originator_host_uuid)
    {
        Util.logger_assert("Unexpected method on DeserializationEvent");
    }

}