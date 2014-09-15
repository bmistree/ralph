package RalphVersions;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.SortedSet;

import ralph_local_version_protobuffs.DeltaProto.Delta;
import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

public class ObjectHistory
{
    final public SortedSet history = new TreeSet<SingleObjectChange>(
        ROOT_COMMIT_LAMPORT_TIME_COMPARATOR);
    final String object_uuid;

    public ObjectContents initial_construction_contents = null;

    public ObjectHistory(String object_uuid)
    {
        this.object_uuid = object_uuid;
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


    private class SingleObjectChange
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