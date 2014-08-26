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

    public void add_device_update(SingleDeviceUpdate update)
    {
        update_set.add(update);
    }

    public SingleDeviceUpdateListMessage.Builder produce_msg()
    {
        SingleDeviceUpdateListMessage.Builder to_return =
            SingleDeviceUpdateListMessage.newBuilder();
        to_return.setDeviceId(device_id);

        for (SingleDeviceUpdate update : update_set)
        {
            to_return.addUpdateList(
                update.produce_msg());
        }
        return to_return;
    }
}