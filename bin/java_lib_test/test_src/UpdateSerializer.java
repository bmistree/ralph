package java_lib_test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.ByteBuffer;

import com.google.protobuf.ByteString;

import ralph_version_protobuffs.SingleDeviceUpdateProto.SingleDeviceUpdateMessage;

import RalphVersions.SingleDeviceUpdate;
import RalphVersions.IDeviceSpecificUpdateSerializer;


public class UpdateSerializer
{
    private final static String test_name = "UpdateSerializer";
    private final static int SIZE_OF_INT_IN_BYTES = 4;
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);
    private final static Random rand = new Random();
    
    public static void main(String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        for (int i = 0; i < 1000; ++i)
        {
            int to_test = rand.nextInt();
            if (! run_single_test(to_test))
                return false;
        }
        return true;
    }
    
    private static class IntegerDeviceUpdateSerializer
        implements IDeviceSpecificUpdateSerializer<Integer>
    {
        @Override
        public ByteString serialize(Integer to_serialize)
        {
            ByteBuffer bb = ByteBuffer.allocate(SIZE_OF_INT_IN_BYTES);
            bb.putInt(to_serialize);
            bb.rewind();
            return ByteString.copyFrom(bb);
        }
    }
    
    public static boolean run_single_test(Integer update_data)
    {
        String device_id = "device";
        long root_commit_lamport_time = 1L;
        long local_lamport_time = 2L;
        String root_application_uuid = "root_app_uuid";
        String event_name = "event_name";
        String event_uuid = "event_uuid";

        SingleDeviceUpdate<Integer> sd_update =
            generate_int_single_device_update(
                device_id, root_commit_lamport_time,
                local_lamport_time, root_application_uuid,
                event_name, event_uuid, update_data);
            
        SingleDeviceUpdateMessage.Builder builder = sd_update.produce_msg();
        SingleDeviceUpdateMessage msg = builder.build();

        Integer deserialized_data = produce_data_from_device_update(msg);

        if (had_exception.get())
            return false;
        
        return deserialized_data.equals(update_data);
    }
    
    public static SingleDeviceUpdate<Integer> generate_int_single_device_update(
        String device_id, long root_commit_lamport_time,
        long local_lamport_time, String root_application_uuid,
        String event_name,String event_uuid,
        Integer update_data)
    {
        SingleDeviceUpdate<Integer> sd_update =
            new SingleDeviceUpdate (
                device_id,
                SingleDeviceUpdate.UpdateType.STAGE,
                update_data,
                root_commit_lamport_time,local_lamport_time,
                root_application_uuid,event_name,event_uuid,
                new IntegerDeviceUpdateSerializer());

        return sd_update;
    }

    public static Integer produce_data_from_device_update(
        SingleDeviceUpdateMessage msg)
    {
        ByteString msg_data = msg.getUpdateMsgData();
        ByteBuffer bb = msg_data.asReadOnlyByteBuffer();
        int to_return = -1;
        try
        {
            to_return = bb.getInt();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            had_exception.set(true);
        }
        return to_return;
    }
}