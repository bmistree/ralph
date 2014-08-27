package java_lib_test;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import ralph.LamportClock;
import ralph.RalphGlobals;

import RalphVersions.VersionManager;
import RalphVersions.SingleDeviceUpdate;
import RalphVersions.SingleDeviceUpdateList;


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
    private final static int num_updates_to_test = 1000;
    private final static Random rand = new Random();
    
    public static void main(String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(TEST_NAME);
        else
            TestClassUtil.print_failure(TEST_NAME);
    }

    public static boolean run_test()
    {
        RalphGlobals ralph_globals = new RalphGlobals();

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

        
        return true;
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
            
            to_return.add(update_value);
        }
        return to_return;
    }
}