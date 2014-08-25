package RalphVersions;

import java.util.Comparator;

/**
   All the info for a single update to a single device.  Parameterized
   by the type of the update.  For instance, a SwitchUpdate in
   pronghorn would likely have the signature:

   public SwitchUpdate extends SingleDeviceUpdate<FTableUpdate> {...}
 */
public class SingleDeviceUpdate<DeviceUpdateType>
{
    public enum UpdateType
    {
        STAGE, COMPLETE, BACKOUT
    }

    /**
       Should be unique to device
     */
    final public String device_id;
    final public UpdateType update_type;
    final public DeviceUpdateType device_update;
    /**
       When root issued a commit request, what time did its lamport
       clock read?
     */
    final public String root_commit_lamport_time;
    /**
       When processing request, what time was it locally?
     */
    final public String local_lamport_time;
    final public String root_application_uuid;
    /**
       What was the name of the method that the root executed to get
       here?
     */
    final public String event_name;
    /**
       What was the uuid of the event that this message is associated
       with.  Note: can have multiple SingleDeviceUpdates for the same
       device_id with the same event_uuid.  This is because keeping
       track of network state from point staged commit to time
       completed commit.
     */
    final public String event_uuid;
    final private IDeviceSpecificUpdateSerializer<DeviceUpdateType>
        update_serializer;

    
    public SingleDeviceUpdate(
        String _device_id, UpdateType _update_type,
        DeviceUpdateType _device_update, String _root_commit_lamport_time,
        String _local_lamport_time,String _root_application_uuid,
        String _event_name,String _event_uuid,
        IDeviceSpecificUpdateSerializer<DeviceUpdateType> _update_serializer)
    {
        device_id = _device_id;
        update_type = _update_type;
        device_update = _device_update;
        root_commit_lamport_time = _root_commit_lamport_time;
        local_lamport_time = _local_lamport_time;
        root_application_uuid = _root_application_uuid;
        event_name = _event_name;
        event_uuid = _event_uuid;
        update_serializer = _update_serializer;
    }

    private static class RootCommitLamportTimeComparator
        implements Comparator<SingleDeviceUpdate>
    {
        @Override
        public int compare(SingleDeviceUpdate a, SingleDeviceUpdate b)
        {
            return a.root_commit_lamport_time.compareTo(
                b.root_commit_lamport_time);
        }
    }
    private static class LocalLamportTimeComparator
        implements Comparator<SingleDeviceUpdate>
    {
        @Override
        public int compare(SingleDeviceUpdate a, SingleDeviceUpdate b)
        {
            return a.local_lamport_time.compareTo(b.local_lamport_time);
        }
    }

    public static RootCommitLamportTimeComparator ROOT_COMMIT_LAMPORT_TIME_COMPARATOR =
        new RootCommitLamportTimeComparator();
    public static LocalLamportTimeComparator LOCAL_LAMPORT_TIME_COMPARATOR =
        new LocalLamportTimeComparator();
}
