package RalphVersions;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

import ralph.LamportClock;

import ralph_version_protobuffs.VersionMessageProto.VersionMessage;
import ralph_version_protobuffs.VersionRequestProto.VersionRequestMessage;
import ralph_version_protobuffs.VersionResponseProto.VersionResponseMessage;

public class VersionManager implements IVersionManager
{
    private final HashMap<String,SingleDeviceUpdateList>
        device_id_to_update_list =
            new HashMap<String,SingleDeviceUpdateList>();

    // argument true ensures that access to read-write locks is fair.
    private final ReentrantReadWriteLock rwl =
        new ReentrantReadWriteLock(true);
    private final Lock r_lock = rwl.readLock();
    private final Lock w_lock = rwl.writeLock();

    private final LamportClock clock;
    
    public VersionManager(LamportClock _clock)
    {
        clock = _clock;
    }
    
    @Override
    public VersionMessage.Builder produce_response(
        VersionRequestMessage request)
    {
        VersionResponseMessage.Builder version_response =
            VersionResponseMessage.newBuilder();
        version_response.setQueryId(request.getQueryId());

        boolean specific_device_query = request.getDeviceIdListCount() != 0;

        Long lower_root_commit_lamport_time = null;
        Long upper_root_commit_lamport_time = null;
        Long lower_local_lamport_time = null;
        Long upper_local_lamport_time = null;
        String root_application_uuid = null;
        String event_name = null;
        String event_uuid = null;

        if (request.hasLowerRootCommitLamportTime())
        {
            lower_root_commit_lamport_time =
                request.getLowerRootCommitLamportTime();
        }
        if (request.hasUpperRootCommitLamportTime())
        {
            upper_root_commit_lamport_time =
                request.getUpperRootCommitLamportTime();
        }
        if (request.hasLowerLocalLamportTime())
        {
            lower_local_lamport_time =
                request.getLowerLocalLamportTime();
        }
        if (request.hasUpperLocalLamportTime())
        {
            upper_local_lamport_time =
                request.getUpperLocalLamportTime();
        }
        if (request.hasEventName())
            event_name = request.getEventName();
        if (request.hasEventUuid())
            event_uuid = request.getEventUuid().getData();


        // actually form responses for each device
        r_lock.lock();
        Collection<SingleDeviceUpdateList> device_query_collection = null;
        if (specific_device_query)
        {
            // not querying over all devices, just querying on
            // targetted device ids.  
            List<SingleDeviceUpdateList> to_query_over =
                new ArrayList<SingleDeviceUpdateList>();
            for (String device_id : request.getDeviceIdListList())
            {
                SingleDeviceUpdateList sdul = device_id_to_update_list.get(device_id);
                if (sdul != null)
                    to_query_over.add(sdul);
            }
        }
        else
        {
            device_query_collection = device_id_to_update_list.values();
        }
        
        for (SingleDeviceUpdateList sdul : device_query_collection)
        {
            version_response.addDeviceList(
                sdul.produce_msg(
                    lower_root_commit_lamport_time,
                    upper_root_commit_lamport_time,
                    lower_local_lamport_time,
                    upper_local_lamport_time,
                    root_application_uuid,event_name,event_uuid));
        }
        r_lock.unlock();

        // send message back
        VersionMessage.Builder to_return = VersionMessage.newBuilder();
        to_return.setResponse(version_response);
        to_return.setTimestamp(clock.get_int_timestamp());
        return to_return;
    }
    
    @Override
    public void add_single_device_update_list(SingleDeviceUpdateList sdul)
    {
        w_lock.lock();
        device_id_to_update_list.put(sdul.device_id,sdul);
        w_lock.unlock();
    }
}