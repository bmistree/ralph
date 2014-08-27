package RalphVersions;

import ralph.CommitMetadata;
import ralph.LamportClock;

public class VersionListener<DataType> implements IVersionListener<DataType>
{
    final private LamportClock clock;
    final private IVersionManager version_manager;
    final public String device_id;
    final private SingleDeviceUpdateList single_device_update_list;
    final private IDeviceSpecificUpdateSerializer<DataType> update_serializer;
    
    public VersionListener(
        LamportClock _clock, IVersionManager _version_manager,
        String _device_id,
        IDeviceSpecificUpdateSerializer<DataType> _update_serializer)
    {
        clock = _clock;
        version_manager = _version_manager;
        device_id = _device_id;
        single_device_update_list = new SingleDeviceUpdateList(_device_id);
        update_serializer = _update_serializer;

        // make version server aware that it can serve responses for
        // this device.
        version_manager.add_single_device_update_list(
            single_device_update_list);
    }
    
    public void stage_delta(DataType staged_delta,CommitMetadata metadata)
    {
        long local_lamport_time =
            clock.get_and_increment_int_timestamp();

        SingleDeviceUpdate update = new SingleDeviceUpdate(
            device_id,SingleDeviceUpdate.UpdateType.STAGE,
            staged_delta,metadata.root_commit_lamport_time,
            local_lamport_time,metadata.root_application_uuid,
            metadata.event_name,metadata.event_uuid,
            update_serializer);

        single_device_update_list.add_device_update(update);
    }
    
    /**
       Do not require the completed data because should already have a
       record of it from stage_delta.
     */
    public void complete_delta(CommitMetadata metadata)
    {
        long local_lamport_time =
            clock.get_and_increment_int_timestamp();
        SingleDeviceUpdate update = new SingleDeviceUpdate(
            device_id,SingleDeviceUpdate.UpdateType.COMPLETE,
            null, // should be able to recover delta from the
                  // stage_delta call that's already been saved.
            metadata.root_commit_lamport_time,
            local_lamport_time,metadata.root_application_uuid,
            metadata.event_name,metadata.event_uuid,
            update_serializer);

        single_device_update_list.add_device_update(update);
    }
    
    /**
       Only called for versions that have been staged, and
       subsequently backed out.  Not for versions that have been
       backed out before being staged.

       Do not require the completed data because should already have a
       record of it from stage_delta.
     */
    public void backout_delta(CommitMetadata metadata)
    {
        long local_lamport_time =
            clock.get_and_increment_int_timestamp();
        SingleDeviceUpdate update = new SingleDeviceUpdate(
            device_id,SingleDeviceUpdate.UpdateType.BACKOUT,
            null, // should be able to recover delta from the
                  // stage_delta call that's already been saved.
            metadata.root_commit_lamport_time,
            local_lamport_time,metadata.root_application_uuid,
            metadata.event_name,metadata.event_uuid,
            update_serializer);

        single_device_update_list.add_device_update(update);
    }
}