package RalphVersions;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.SortedSet;

import ralph_version_protobuffs.SingleDeviceUpdateListProto.SingleDeviceUpdateListMessage;

/**
   Should be extended, eg., to serialize a message to be sent over the
   wire.
 */
public class SingleDeviceVersionResponse
{
    /**
       Should be unique to device
     */
    final public String device_id;

    final private SortedSet<SingleDeviceUpdate> update_set =
        new TreeSet<SingleDeviceUpdate>(
            SingleDeviceUpdate.ROOT_COMMIT_LAMPORT_TIME_COMPARATOR);

    public SingleDeviceVersionResponse(String _device_id)
    {
        device_id = _device_id;
    }

    synchronized public void add_device_update(SingleDeviceUpdate update)
    {
        update_set.add(update);
    }

    /**
       If any argument is null, means do not use it to filter
       updates.
     */
    synchronized public SingleDeviceUpdateListMessage.Builder
        produce_msg(Long lower_root_commit_lamport_time,
                    Long upper_root_commit_lamport_time,
                    Long lower_local_lamport_time,
                    Long upper_local_lamport_time,
                    String root_application_uuid,
                    String event_name,String event_uuid)
    {
        // FIXME: longs are signed.  Given that, check that the > <
        // comparisons below are valid.
        
        SingleDeviceUpdateListMessage.Builder to_return =
            SingleDeviceUpdateListMessage.newBuilder();
        to_return.setDeviceId(device_id);

        for (SingleDeviceUpdate update : update_set)
        {
            // filter events that do not match root_commit_lamport_time query
            if ((lower_root_commit_lamport_time != null) &&
                (lower_root_commit_lamport_time > update.root_commit_lamport_time))
            {
                continue;
            }
            if ((upper_root_commit_lamport_time != null) &&
                (upper_root_commit_lamport_time < update.root_commit_lamport_time))
            {
                continue;
            }
            // filter events that do not match local_lamport_time query
            if ((lower_local_lamport_time != null) &&
                (lower_local_lamport_time > update.local_lamport_time))
            {
                continue;
            }
            if ((upper_local_lamport_time != null) &&
                (upper_local_lamport_time < update.local_lamport_time))
            {
                continue;
            }

            // filter events that do not match root_application_uuid
            if ((root_application_uuid != null) &&
                (!root_application_uuid.equals(update.root_application_uuid)))
            {
                continue;
            }
            // filter event name
            if ((event_name != null) &&
                (! event_name.equals(update.event_name)))
            {
                continue;
            }

            // filter event_uuid
            if ((event_uuid != null) &&
                (! event_uuid.equals(update.event_uuid)))
            {
                continue;
            }

            to_return.addUpdateList(update.produce_msg());
        }
        return to_return;
    }    
    
}