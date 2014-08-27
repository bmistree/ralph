package java_lib_test;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.net.Socket;
import java.io.IOException;

import ralph_version_protobuffs.VersionMessageProto.VersionMessage;
import ralph_version_protobuffs.VersionRequestProto.VersionRequestMessage;
import ralph_version_protobuffs.VersionResponseProto.VersionResponseMessage;
import ralph_version_protobuffs.SingleDeviceUpdateListProto.SingleDeviceUpdateListMessage;
import ralph_version_protobuffs.SingleDeviceUpdateProto.SingleDeviceUpdateMessage;

import ralph.LamportClock;
import ralph.RalphGlobals;

import RalphVersions.VersionManager;
import RalphVersions.IVersionManager;
import RalphVersions.SingleDeviceUpdate;
import RalphVersions.SingleDeviceUpdateList;
import RalphVersions.VersionServer.ServerThread;


/**
   This test creates a list of updates for some fake device.  Then, it
   creates a version server, which serves these updates.  Following,
   we connect to the version server, query it, and check to ensure
   that the query response is what we expect.
 */
public class VersionQuerier
{
    private final static String TEST_NAME = "VersionQuerier";
    private final static String DEVICE_ID = "dummy_device_id";
    private final static String ROOT_APPLICATION_UUID = "root_app_uuid";
    private final static String EVENT_NAME = "event_name";
    private final static String EVENT_UUID = "event_uuid";

    private final static String VERSION_SERVER_IP_ADDRESS = "127.0.0.1";
    private final static int VERSION_SERVER_PORT_NUMBER = 39201;
    
    private final static int num_updates_to_test = 1000;
    private final static Random rand = new Random();

    private final static RalphGlobals ralph_globals = new RalphGlobals();
    
    public static void main(String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(TEST_NAME);
        else
            TestClassUtil.print_failure(TEST_NAME);
    }

    public static boolean run_test()
    {
        // create version manager and put a device update list in it.
        VersionManager version_manager = 
            new VersionManager(ralph_globals.clock);
        SingleDeviceUpdateList single_device_update_list =
            new SingleDeviceUpdateList(DEVICE_ID);
        version_manager.add_single_device_update_list(
            single_device_update_list);

        
        // put random state into device update list
        List<Integer> update_values =
            insert_random_updates(single_device_update_list);


        // actually start running the version server
        start_version_server(version_manager);

        List<Integer> received_update_values = query_for_updates();
        if (! received_update_values.equals(update_values))
            return false;
        
        return true;
    }

    public static void start_version_server(IVersionManager version_manager)
    {
        ServerThread server_thread =
            new ServerThread(
                version_manager, VERSION_SERVER_IP_ADDRESS,
                VERSION_SERVER_PORT_NUMBER);
        server_thread.start();
    }

    /**
       Returns null if there's an error.
     */
    private static List<Integer> query_for_updates()
    {
        Socket sock = null;
        try
        {
            sock = new Socket(
                VERSION_SERVER_IP_ADDRESS,VERSION_SERVER_PORT_NUMBER);
            sock.setTcpNoDelay(true);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        Long query_id = 1L;
        VersionRequestMessage.Builder request =
            VersionRequestMessage.newBuilder();
        request.setQueryId(query_id);

        VersionMessage.Builder vm_builder = VersionMessage.newBuilder();
        vm_builder.setRequest(request);
        vm_builder.setTimestamp(ralph_globals.clock.get_int_timestamp());
        VersionMessage version_message = vm_builder.build();

        // send query to version manager
        try
        {
            version_message.writeDelimitedTo(sock.getOutputStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }

        // listen for response
        VersionMessage vm = null;
        try
        {
            vm = VersionMessage.parseDelimitedFrom(
                sock.getInputStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        
        if (! vm.hasResponse())
            return null;
        
        List<Integer> to_return = new ArrayList<Integer>();
        
        VersionResponseMessage response_message = vm.getResponse();
        for (SingleDeviceUpdateListMessage update_list_msg :
                 response_message.getDeviceListList())
        {
            for (SingleDeviceUpdateMessage update_msg :
                     update_list_msg.getUpdateListList())
            {
                Integer returned =
                    UpdateSerializer.produce_data_from_device_update(
                        update_msg);
                to_return.add(returned);
            }
        }
        return to_return;
    }

    
    /**
       Returns a list of integers that contains the updates pushed to
       the device in order of their root_commit_lamport_times.
     */
    private static List<Integer> insert_random_updates(
        SingleDeviceUpdateList single_device_update_list)
    {
        List<Integer> to_return = new ArrayList<Integer>();
        for (int i = 0; i < num_updates_to_test; ++i)
        {
            int update_value = rand.nextInt();
            SingleDeviceUpdate<Integer> sd_update =
                UpdateSerializer.generate_int_single_device_update(
                    DEVICE_ID,
                    i, // root_commit_lamport_time
                    i, // local_lamport_time
                    ROOT_APPLICATION_UUID,
                    EVENT_NAME,
                    EVENT_UUID,
                    update_value);

            single_device_update_list.add_device_update(sd_update);
            to_return.add(update_value);
        }
        return to_return;
    }
}