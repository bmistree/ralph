package java_lib_test;

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
    
    public static void main(String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    private static class IntegerDeviceUpdateSerializer
        implements IDeviceSpecificUpdateSerializer<Integer>
    {
        @Override
        public ByteString serialize(Integer to_serialize)
        {
            ByteBuffer bb = ByteBuffer.allocate(SIZE_OF_INT_IN_BYTES);
            bb.putInt(to_serialize);
            return ByteString.copyFrom(bb);
        }
    }
    
    public static boolean run_test()
    {
        String device_id = "device";
        long root_commit_lamport_time = 1L;
        long local_lamport_time = 2L;
        String root_application_uuid = "root_app_uuid";
        String event_name = "event_name";
        String event_uuid = "event_uuid";

        Integer update_data = 1;
        
        SingleDeviceUpdate<Integer> sd_update =
            new SingleDeviceUpdate (
                device_id,
                SingleDeviceUpdate.UpdateType.STAGE,
                update_data,
                root_commit_lamport_time,local_lamport_time,
                root_application_uuid,event_name,event_uuid,
                new IntegerDeviceUpdateSerializer());
        
        SingleDeviceUpdateMessage.Builder builder = sd_update.produce_msg();
        SingleDeviceUpdateMessage msg = builder.build();


        Integer deserialized_data = produce_data_from_device_update(msg);

        if (had_exception.get())
            return false;
        
        return deserialized_data.equals(update_data);
    }

    private static Integer produce_data_from_device_update(
        SingleDeviceUpdateMessage msg)
    {
        ByteString msg_data = msg.getUpdateMsgData();
        ByteBuffer bb = msg_data.asReadOnlyByteBuffer();
        int to_return = -1;
        try
        {
            to_return = bb.getInt();
        }
        catch(Exception _ex)
        {
            ex.printStackTrace();
            had_exception.set(true);
        }
        return to_return;
    }
}