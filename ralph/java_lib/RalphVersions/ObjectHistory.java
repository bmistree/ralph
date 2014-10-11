package RalphVersions;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Set;

import com.google.protobuf.ByteString;

import ralph.RalphObject;
import ralph.RalphInternalMapInterface;
import ralph.RalphInternalListInterface;
import ralph.EnumConstructorObj;
import ralph.IInternalReferenceHolder;
import ralph.Util;
import ralph.InternalServiceFactory;
import ralph.InternalServiceReference;
import ralph.RalphGlobals;
import ralph.ActiveEvent;

import RalphExceptions.BackoutException;

import ralph_protobuffs.DeltaProto.Delta;
import ralph_protobuffs.DeltaProto.Delta.ValueType;
import ralph_protobuffs.DeltaProto.Delta.ContainerOpType;
import ralph_protobuffs.DeltaProto.Delta.ContainerDelta;
import ralph_protobuffs.ObjectContentsProto.ObjectContents;

public class ObjectHistory
{
    final public SortedSet<SingleObjectChange> history =
        new TreeSet<SingleObjectChange>(
            ROOT_COMMIT_LAMPORT_TIME_COMPARATOR);
    final String object_uuid;

    public ObjectContents initial_construction_contents = null;

    public ObjectHistory(String object_uuid)
    {
        this.object_uuid = object_uuid;
    }

    /**
       @param lower_range --- null if should query from earliest
       record.

       @param upper_range --- null if should query to latest record.
       
       @returns ObjectHistory object with SingleObjectChange values
       only between lower_range and upper_range.
     */
    public ObjectHistory produce_range(Long lower_range,Long upper_range)
    {
        ObjectHistory to_return = new ObjectHistory(object_uuid);

        // FIXME: linear search for start.  Probably could use binary
        // search instead.
        for (SingleObjectChange change : history)
        {
            if ((lower_range != null) &&
                (change.root_lamport_time < lower_range))
            {
                continue;
            }
            if ((upper_range != null) &&
                (change.root_lamport_time > upper_range))
            {
                break;
            }

            to_return.history.add(change);
        }
        return to_return;
    }
    
    public ObjectContents get_construction_contents()
    {
        return initial_construction_contents;
    }
    
    public void set_construction_contents(ObjectContents contents)
    {
        initial_construction_contents = contents;
    }

    public void add_delta (
        long root_lamport_time, Delta delta,
        String commit_metadata_event_uuid)
    {
        history.add(
            new SingleObjectChange(
                root_lamport_time,delta,commit_metadata_event_uuid));
    }

    // FIXME: for all replayers, could probably just zoom to last
    // value and set there instead of setting each time in for loop.

    /**
       Sets the ref_to_replay_from field of reference.
     */
    public static String find_reference(
        ObjectHistory obj_history,Long to_play_until)
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return null;

        return change.delta.getReference().getReference();
    }

    protected static SingleObjectChange find_target_single_object_change(
        ObjectHistory obj_history, Long to_play_until)
    {
        SingleObjectChange to_return = null;
        for (SingleObjectChange change : obj_history.history)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return to_return;
            }
            to_return = change;
        }
        return to_return;
    }
    
    public static void replay_internal_map(
        RalphInternalMapInterface to_replay_on,
        ObjectHistory obj_history, Long to_play_until,
        IReconstructionContext reconstruction_context)
    {
        try
        {
            deserialize_internal_map(
                to_replay_on, obj_history,to_play_until,
                reconstruction_context,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    /**
       If act_event is null, will direct_set_val on object instead of
       set_val.
     */
    public static void deserialize_internal_map(
        RalphInternalMapInterface to_replay_on,
        ObjectHistory obj_history, Long to_play_until,
        IReconstructionContext reconstruction_context,
        ActiveEvent act_event) throws BackoutException
    {        
        for (SingleObjectChange change : obj_history.history)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return;
            }
            SingleObjectChange.internal_map_incorporate_single_object_change(
                change,to_replay_on,reconstruction_context,
                change.root_lamport_time,act_event);
        }
    }

    public static void replay_internal_list(
        RalphInternalListInterface to_replay_on,
        ObjectHistory obj_history, Long to_play_until,
        IReconstructionContext reconstruction_context)
    {
        try
        {
            deserialize_internal_list(
                to_replay_on,obj_history,to_play_until,
                reconstruction_context,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    
    public static void deserialize_internal_list(
        RalphInternalListInterface to_replay_on,
        ObjectHistory obj_history, Long to_play_until,
        IReconstructionContext reconstruction_context,
        ActiveEvent act_event) throws BackoutException
    {
        for (SingleObjectChange change : obj_history.history)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return;
            }
            SingleObjectChange.internal_list_incorporate_single_object_change(
                change,to_replay_on,reconstruction_context,
                change.root_lamport_time,act_event);
        }
    }
    
    public static void replay_number(
        RalphObject<Double,Double> to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        try
        {
            deserialize_number(to_replay_on,obj_history,to_play_until,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    public static void deserialize_number(
        RalphObject<Double,Double> to_replay_on,
        ObjectHistory obj_history,Long to_play_until,
        ActiveEvent act_event) throws BackoutException
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return;

        SingleObjectChange.number_incorporate_single_object_change(
            change,to_replay_on,act_event);
    }

    
    public static void replay_service_reference(
        RalphObject<InternalServiceReference,InternalServiceReference> to_replay_on,
        ObjectHistory obj_history,Long to_play_until, RalphGlobals ralph_globals)
    {
        try
        {
            deserialize_service_reference(
                to_replay_on, obj_history, to_play_until, ralph_globals,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    public static void deserialize_service_reference(
        RalphObject<InternalServiceReference,InternalServiceReference> to_replay_on,
        ObjectHistory obj_history,Long to_play_until, RalphGlobals ralph_globals,
        ActiveEvent act_event) throws BackoutException
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return;

        SingleObjectChange.service_reference_incorporate_single_object_change(
            change,to_replay_on,ralph_globals,act_event);
    }

    
    public static void replay_service_factory(
        RalphObject<InternalServiceFactory,InternalServiceFactory> to_replay_on,
        ObjectHistory obj_history, Long to_play_until,RalphGlobals ralph_globals)
    {
        try
        {
            deserialize_service_factory(
                to_replay_on, obj_history, to_play_until, ralph_globals,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    public static void deserialize_service_factory(
        RalphObject<InternalServiceFactory,InternalServiceFactory> to_replay_on,
        ObjectHistory obj_history, Long to_play_until,RalphGlobals ralph_globals,
        ActiveEvent act_event) throws BackoutException
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return;

        SingleObjectChange.service_factory_incorporate_single_object_change(
            change,to_replay_on,ralph_globals,act_event);
    }

    
    
    public static <EnumType extends Enum> void replay_enum(
        RalphObject<EnumType,EnumType> to_replay_on,
        ObjectHistory obj_history,Long to_play_until,
        IVersionReplayer replayer)
    {
        try
        {
            ObjectHistory.<EnumType>deserialize_enum(
                to_replay_on,obj_history,to_play_until,replayer,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    public static <EnumType extends Enum> void deserialize_enum(
        RalphObject<EnumType,EnumType> to_replay_on,
        ObjectHistory obj_history,Long to_play_until,
        IVersionReplayer replayer, ActiveEvent act_event)
        throws BackoutException
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return;

        SingleObjectChange.<EnumType>enum_incorporate_single_object_change(
            change,to_replay_on,replayer,act_event);
    }

    public static void replay_text(
        RalphObject<String,String> to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        try
        {
            deserialize_text(
                to_replay_on,obj_history,to_play_until,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    public static void deserialize_text(
        RalphObject<String,String> to_replay_on,
        ObjectHistory obj_history,Long to_play_until, ActiveEvent act_event)
        throws BackoutException
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return;

        SingleObjectChange.text_incorporate_single_object_change(
            change,to_replay_on,act_event);
    }

    
    public static void replay_tf(
        RalphObject<Boolean,Boolean> to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        try
        {
            deserialize_tf(
                to_replay_on,obj_history,to_play_until,null);
        }
        catch (BackoutException ex)
        {
            ex.printStackTrace();
            Util.logger_assert(
                "Should be unable to throw backout when replaying");
        }
    }

    public static void deserialize_tf(
        RalphObject<Boolean,Boolean> to_replay_on,
        ObjectHistory obj_history,Long to_play_until, ActiveEvent act_event)
        throws BackoutException
    {
        SingleObjectChange change =
            find_target_single_object_change(obj_history,to_play_until);

        if (change == null)
            return;
        
        SingleObjectChange.true_false_incorporate_single_object_change(
            change,to_replay_on,act_event);
    }

    
    
    public static class SingleObjectChange
    {
        public final long root_lamport_time;
        public final Delta delta;
        public final String commit_metadata_event_uuid;

        public SingleObjectChange(
            long root_lamport_time, Delta delta,
            String commit_metadata_event_uuid)
        {
            this.root_lamport_time = root_lamport_time;
            this.delta = delta;
            this.commit_metadata_event_uuid = commit_metadata_event_uuid;
        }

        public static void service_reference_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<InternalServiceReference,InternalServiceReference> to_incorporate_into,
            RalphGlobals ralph_globals, ActiveEvent act_event) throws BackoutException
        {
            InternalServiceReference internal_service_reference =
                InternalServiceReference.deserialize_delta (
                    change.delta.getServiceReferenceDelta());
            if (act_event == null)
                to_incorporate_into.direct_set_val(internal_service_reference);
            else
            {
                to_incorporate_into.set_val(
                    act_event,internal_service_reference);
            }
        }

        public static void number_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<Double,Double> to_incorporate_into,
            ActiveEvent act_event) throws BackoutException
        {
            double internal_number = change.delta.getValue().getNum();
            if (act_event == null)
                to_incorporate_into.direct_set_val(internal_number);
            else
                to_incorporate_into.set_val(act_event,internal_number);
        }
        
        public static <EnumType extends Enum> void enum_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<EnumType,EnumType> to_incorporate_into,
            IVersionReplayer replayer, ActiveEvent act_event)
            throws BackoutException
        {
            Delta.EnumDelta enum_delta = change.delta.getEnumDelta();
            String enum_constructor_obj_class_name =
                enum_delta.getEnumConstructorObjClassName();
            int enum_ordinal = enum_delta.getEnumOrdinal();
            
            EnumConstructorObj<EnumType> enum_constructor =
                (EnumConstructorObj<EnumType>) replayer.get_enum_constructor_obj(
                    enum_constructor_obj_class_name);

            EnumType internal_enum =
                enum_constructor.construct_enum(enum_ordinal);
            if (act_event == null)
                to_incorporate_into.direct_set_val(internal_enum);
            else
                to_incorporate_into.set_val(act_event,internal_enum);
        }

        
        public static void service_factory_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<InternalServiceFactory,InternalServiceFactory>to_replay_on,
            RalphGlobals ralph_globals,ActiveEvent act_event)
            throws BackoutException
        {
            ByteString serialized_delta =
                change.delta.getServiceFactoryDelta().getSerializedFactory();

            InternalServiceFactory internal_service_factory =
                InternalServiceFactory.deserialize (serialized_delta,ralph_globals);
            if (act_event == null)
                to_replay_on.direct_set_val(internal_service_factory);
            else
                to_replay_on.set_val(act_event,internal_service_factory);
        }
        
        public static void text_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<String,String> to_incorporate_into,
            ActiveEvent act_event) throws BackoutException
        {
            String internal_string = change.delta.getValue().getText();
            if (act_event == null)
                to_incorporate_into.direct_set_val(internal_string);
            else
                to_incorporate_into.set_val(act_event,internal_string);
        }

        public static void true_false_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<Boolean,Boolean> to_incorporate_into,
            ActiveEvent act_event)
            throws BackoutException
        {
            boolean internal_bool = change.delta.getValue().getTf();
            if (act_event == null)
                to_incorporate_into.direct_set_val(internal_bool);
            else
                to_incorporate_into.set_val(act_event,internal_bool);
        }

        public static void internal_map_incorporate_single_object_change(
            SingleObjectChange change, RalphInternalMapInterface to_replay_on,
            IReconstructionContext reconstruction_context,
            Long lamport_timestamp_before_or_during, ActiveEvent act_event)
            throws BackoutException
        {
            for (ContainerDelta delta : change.delta.getContainerDeltaList())
            {
                if (ContainerOpType.CLEAR == delta.getOpType())
                {
                    if (act_event == null)
                        to_replay_on.direct_clear();
                    else
                        to_replay_on.clear(act_event);
                }
                else
                {
                    ValueType key_wrapper = delta.getKey();
                    Object key = null;
                    if (key_wrapper.hasNum())
                        key = key_wrapper.getNum();
                    else if (key_wrapper.hasText())
                        key = key_wrapper.getText();
                    else if (key_wrapper.hasTf())
                        key = key_wrapper.getTf();
                    //// DEBUG
                    else
                        Util.logger_assert("Unknown key wrapper");
                    //// END DEBUG
                    
                    if (ContainerOpType.DELETE == delta.getOpType())
                    {
                        if (act_event == null)
                            to_replay_on.direct_remove_val_on_key(key);
                        else
                            to_replay_on.remove(act_event,key);
                    }
                    else
                    {
                        String internal_obj_uuid =
                            delta.getWhatAddedOrWritten().getReference();
                        RalphObject new_value =
                            reconstruction_context.get_constructed_object(
                                internal_obj_uuid,
                                lamport_timestamp_before_or_during);

                        if ((ContainerOpType.ADD == delta.getOpType()) ||
                            (ContainerOpType.WRITE == delta.getOpType()))
                        {
                            if (act_event == null)
                            {
                                to_replay_on.direct_set_val_on_key(
                                    key,new_value);
                            }
                            else
                            {
                                to_replay_on.set_val_on_key(
                                    act_event, key,new_value);
                            }
                        }
                        //// DEBUG
                        else
                        {
                            Util.logger_assert("Unknown op type on map.");
                        }
                        //// END DEBUG
                    }
                }
            }
        }

        public static void internal_list_incorporate_single_object_change(
            SingleObjectChange change, RalphInternalListInterface to_replay_on,
            IReconstructionContext reconstruction_context,
            Long lamport_timestamp_before_or_during,ActiveEvent act_event)
            throws BackoutException
        {
            for (ContainerDelta delta : change.delta.getContainerDeltaList())
            {
                if (ContainerOpType.CLEAR == delta.getOpType())
                {
                    if (act_event == null)
                        to_replay_on.direct_clear();
                    else
                        to_replay_on.clear(act_event);
                }
                else if (ContainerOpType.ADD == delta.getOpType())
                {
                    String internal_obj_uuid =
                        delta.getWhatAddedOrWritten().getReference();
                    RalphObject new_value =
                        reconstruction_context.get_constructed_object(
                            internal_obj_uuid,
                            lamport_timestamp_before_or_during);

                    if (act_event == null)
                        to_replay_on.direct_append(new_value);
                    else
                        to_replay_on.append(act_event,new_value);
                }
                else
                {
                    ValueType key_wrapper = delta.getKey();
                    Integer key =
                        (new Double(key_wrapper.getNum())).intValue();
                    
                    if (ContainerOpType.DELETE == delta.getOpType())
                    {
                        if (act_event == null)
                            to_replay_on.direct_remove(key);
                        else
                            to_replay_on.remove(act_event,key);
                    }
                    else if (ContainerOpType.WRITE == delta.getOpType())
                    {
                        String internal_obj_uuid =
                            delta.getWhatAddedOrWritten().getReference();
                        RalphObject new_value =
                            reconstruction_context.get_constructed_object(
                                internal_obj_uuid,
                                lamport_timestamp_before_or_during);

                        if (act_event == null)
                        {
                            to_replay_on.direct_set_val_on_key(
                                key,new_value);
                        }
                        else
                        {
                            to_replay_on.set_val_on_key(
                                act_event,key,new_value);
                        }
                    }
                    //// DEBUG
                    else
                    {
                        Util.logger_assert("Unknown op type on list.");
                    }
                    //// END DEBUG
                }
            }
        }
    }

    private static class RootCommitLamportTimeComparator
        implements Comparator<SingleObjectChange>
    {
        @Override
            public int compare(SingleObjectChange a, SingleObjectChange b)
        {
            return Long.valueOf(a.root_lamport_time).compareTo(
                Long.valueOf(b.root_lamport_time));
        }
    }
    public static RootCommitLamportTimeComparator ROOT_COMMIT_LAMPORT_TIME_COMPARATOR =
        new RootCommitLamportTimeComparator();
}