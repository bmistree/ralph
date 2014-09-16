package RalphVersions;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Set;

import ralph.RalphObject;
import RalphExceptions.BackoutException;
import ralph.IReplayableReferenceHolder;

import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

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
    public static void replay_reference(
        IReplayableReferenceHolder to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        for (SingleObjectChange change : obj_history.history)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return;
            }
            SingleObjectChange.reference_holder_incorporate_single_object_change(
                change,to_replay_on);
        }
    }
    
    public static void replay_number(
        RalphObject<Double,Double> to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        Set <SingleObjectChange> single_object_change_set =
            obj_history.history;
        for (SingleObjectChange change : single_object_change_set)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return;
            }
            SingleObjectChange.number_incorporate_single_object_change(
                change,to_replay_on);
        }
    }

    public static void replay_text(
        RalphObject<String,String> to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        Set <SingleObjectChange> single_object_change_set =
            obj_history.history;
        for (SingleObjectChange change : single_object_change_set)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return;
            }

            SingleObjectChange.text_incorporate_single_object_change(
                change,to_replay_on);
        }
    }

    public static void replay_tf(
        RalphObject<Boolean,Boolean> to_replay_on,
        ObjectHistory obj_history,Long to_play_until)
    {
        Set <SingleObjectChange> single_object_change_set =
            obj_history.history;
        for (SingleObjectChange change : single_object_change_set)
        {
            if ((to_play_until != null) &&
                (change.root_lamport_time > to_play_until))
            {
                return;
            }

            SingleObjectChange.true_false_incorporate_single_object_change(
                change,to_replay_on);
        }
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
        
        public static void number_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<Double,Double> to_incorporate_into)
        {
            double internal_number = change.delta.getValue().getNum();
            to_incorporate_into.direct_set_val(internal_number);
        }

        public static void text_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<String,String> to_incorporate_into)
        {
            String internal_string = change.delta.getValue().getText();
            to_incorporate_into.direct_set_val(internal_string);
        }
        
        public static void true_false_incorporate_single_object_change(
            SingleObjectChange change,
            RalphObject<Boolean,Boolean> to_incorporate_into)
        {
            boolean internal_bool = change.delta.getValue().getTf();
            to_incorporate_into.direct_set_val(internal_bool);
        }

        public static void reference_holder_incorporate_single_object_change(
            SingleObjectChange change, IReplayableReferenceHolder holder)
        {
            String ref_to_replay_from =
                change.delta.getReference().getReference();
            holder.set_ref_to_replay_from(ref_to_replay_from);
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