package RalphVersions;

import java.util.Comparator;

import ralph_version_protobuffs.SingleDeviceUpdateProto.SingleDeviceUpdateMessage;
import ralph_version_protobuffs.UtilProto;

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
        STAGE (SingleDeviceUpdateMessage.UpdateType.STAGE),
        COMPLETE (SingleDeviceUpdateMessage.UpdateType.COMPLETE),
        BACKOUT (SingleDeviceUpdateMessage.UpdateType.BACKOUT);

        final private SingleDeviceUpdateMessage.UpdateType proto_update_type;
        // Must use private constructor, because enum.  (Can't let any
        // external code construct a new UpdateType).
        private UpdateType (
            SingleDeviceUpdateMessage.UpdateType _proto_update_type)
        {
            proto_update_type = _proto_update_type;
        }

        public SingleDeviceUpdateMessage.UpdateType protobuf_enum()
        {
            return proto_update_type;
        }
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
    final public long root_commit_lamport_time;
    /**
       When processing request, what time was it locally?
     */
    final public long local_lamport_time;
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
        DeviceUpdateType _device_update, long _root_commit_lamport_time,
        long _local_lamport_time,String _root_application_uuid,
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


    public SingleDeviceUpdateMessage.Builder produce_msg()
    {
        SingleDeviceUpdateMessage.Builder to_return =
            SingleDeviceUpdateMessage.newBuilder();

        // Set nested messages first
        UtilProto.UUID.Builder root_application_uuid_builder =
            UtilProto.UUID.newBuilder();
        root_application_uuid_builder.setData(root_application_uuid);
        UtilProto.UUID.Builder event_uuid_builder =
            UtilProto.UUID.newBuilder();
        event_uuid_builder.setData(event_uuid);
                
        to_return.setRootApplicationUuid(root_application_uuid_builder);
        to_return.setEventUuid(event_uuid_builder);

        // Set all other fields
        to_return.setUpdateType(update_type.protobuf_enum());
        to_return.setRootCommitLamportTime(root_commit_lamport_time);
        to_return.setLocalLamportTime(local_lamport_time);
        to_return.setEventName(event_name);
        to_return.setUpdateMsgData(update_serializer.serialize(device_update));
        
        return to_return;
    }
    
    private static class RootCommitLamportTimeComparator
        implements Comparator<SingleDeviceUpdate>
    {
        @Override
        public int compare(SingleDeviceUpdate a, SingleDeviceUpdate b)
        {
            return Long.valueOf(a.root_commit_lamport_time).compareTo(
                Long.valueOf(b.root_commit_lamport_time));
        }
    }
    private static class LocalLamportTimeComparator
        implements Comparator<SingleDeviceUpdate>
    {
        @Override
        public int compare(SingleDeviceUpdate a, SingleDeviceUpdate b)
        {
            return Long.valueOf(a.local_lamport_time).compareTo(
                Long.valueOf(b.local_lamport_time));
        }
    }

    public static RootCommitLamportTimeComparator ROOT_COMMIT_LAMPORT_TIME_COMPARATOR =
        new RootCommitLamportTimeComparator();
    public static LocalLamportTimeComparator LOCAL_LAMPORT_TIME_COMPARATOR =
        new LocalLamportTimeComparator();
}
