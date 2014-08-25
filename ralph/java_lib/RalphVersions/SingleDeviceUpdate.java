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
    final public String global_lamport_time;
    final public String local_lamport_time;
    final public String root_application_uuid;
    final public String event_name;
    
    public SingleDeviceUpdate(
        String _device_id, UpdateType _update_type,
        DeviceUpdateType _device_update, String _global_lamport_time,
        String _local_lamport_time,String _root_application_uuid,
        String _event_name)
    {
        device_id = _device_id;
        update_type = _update_type;
        device_update = _device_update;
        global_lamport_time = _global_lamport_time;
        local_lamport_time = _local_lamport_time;
        root_application_uuid = _root_application_uuid;
        event_name = _event_name;
    }

    private static class GlobalLamportTimeComparator
        implements Comparator<SingleDeviceUpdate>
    {
        @Override
        public int compare(SingleDeviceUpdate a, SingleDeviceUpdate b)
        {
            return a.global_lamport_time.compareTo(b.global_lamport_time);
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

    public static GlobalLamportTimeComparator GLOBAL_LAMPORT_TIME_COMPARATOR =
        new GlobalLamportTimeComparator();
    public static LocalLamportTimeComparator LOCAL_LAMPORT_TIME_COMPARATOR =
        new LocalLamportTimeComparator();
}
