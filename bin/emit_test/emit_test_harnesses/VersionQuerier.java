package emit_test_harnesses;

import java.util.List;
import java.util.ArrayList;
import java.net.Socket;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.ByteString;

import RalphConnObj.SingleSideConnection;

import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import RalphExtended.ISpeculateListener;
import RalphExtended.ExtendedHardwareOverrides;

import RalphVersions.VersionListener;
import RalphVersions.VersionManager;
import RalphVersions.IVersionManager;
import RalphVersions.IDeviceSpecificUpdateSerializer;
import RalphVersions.VersionServer.ServerThread;

import ralph.RalphGlobals;
import ralph.Variables.AtomicNumberVariable;
import ralph.ICancellableFuture;
import ralph.ActiveEvent;
import ralph.SpeculativeFuture;

import ralph_version_protobuffs.VersionMessageProto.VersionMessage;
import ralph_version_protobuffs.VersionRequestProto.VersionRequestMessage;
import ralph_version_protobuffs.VersionResponseProto.VersionResponseMessage;
import ralph_version_protobuffs.SingleDeviceUpdateListProto.SingleDeviceUpdateListMessage;
import ralph_version_protobuffs.SingleDeviceUpdateProto.SingleDeviceUpdateMessage;

import ralph_emitted.AtomicNumberIncrementerJava.AtomicNumberIncrementer;
import ralph_emitted.AtomicNumberIncrementerJava._InternalWrappedNum;


public class VersionQuerier
{
    private final static RalphGlobals ralph_globals = new RalphGlobals();
    private final static int NUM_TIMES_TO_INCREMENT = 1000;
    private final static String VERSION_SERVER_IP_ADDRESS = "127.0.0.1";
    private final static int VERSION_SERVER_PORT_NUMBER = 39201;
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in VersionQuerier\n");
        else
            System.out.println("\nFAILURE in VersionQuerier\n");
    }


    public static boolean run_test()
    {
        try
        {
            AtomicNumberIncrementer service = new AtomicNumberIncrementer(
                ralph_globals,new SingleSideConnection());

            VersionManager version_manager =
                new VersionManager(ralph_globals.clock);

            _InternalWrappedNum internal_wrapped_num =
                generate_internal_wrapped_num(version_manager);
            
            service.set_wrapped_num(internal_wrapped_num);

            for (int i = 0; i < NUM_TIMES_TO_INCREMENT; ++i)
                service.increment();

            // check that queried updates match
            start_version_server(version_manager);
            
            List<Double> updates = query_for_updates();

            if (updates.size() != NUM_TIMES_TO_INCREMENT)
                return false;

            for (int i = 0; i < NUM_TIMES_TO_INCREMENT; ++i)
            {
                int expected = i + 1;
                Double reported = updates.get(i);

                if (expected != reported.intValue())
                    return false;
            }
            
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }

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
    private static List<Double> query_for_updates()
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
        
        List<Double> to_return = new ArrayList<Double>();
        
        VersionResponseMessage response_message = vm.getResponse();
        for (SingleDeviceUpdateListMessage update_list_msg :
                 response_message.getDeviceListList())
        {
            for (SingleDeviceUpdateMessage update_msg :
                     update_list_msg.getUpdateListList())
            {
                if (update_msg.getUpdateType() ==
                    SingleDeviceUpdateMessage.UpdateType.STAGE)
                {
                    // only stage messages contain data
                    Double returned =
                        produce_data_from_device_update(update_msg);
                    to_return.add(returned);
                }
            }
        }
        return to_return;
    }

    /**
       msg must have STAGE update type.
     */
    public static Double produce_data_from_device_update(
        SingleDeviceUpdateMessage msg)
    {
        ByteString msg_data = msg.getUpdateMsgData();
        ByteBuffer bb = msg_data.asReadOnlyByteBuffer();
        Double to_return = -1.0;
        try
        {
            to_return = bb.getDouble();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            had_exception.set(true);
        }
        return to_return;
    }
    

    public static _InternalWrappedNum generate_internal_wrapped_num(
        VersionManager version_manager)
    {
        VersionListener<Double> version_listener =
            new VersionListener(
                ralph_globals.clock,version_manager, "dummy_device_id",
                new DoubleSerializer());
        
        _InternalWrappedNum to_return = new _InternalWrappedNum(ralph_globals);
        AtomicNumber atomic_number =
            new AtomicNumber(ralph_globals,version_listener);
        to_return.num = atomic_number;
        return to_return;
    }

    private static class SpeculateListener implements ISpeculateListener
    {
        @Override
        public void speculate(ActiveEvent active_event)
        {
            // this test doesn't perform any speculation, so just
            // ignore.
        }
    }

    private static class DoubleSerializer
        implements IDeviceSpecificUpdateSerializer<Double>
    {
        private final int SIZE_OF_DOUBLE_IN_BYTES = 64;
        public ByteString serialize(Double to_serialize)
        {
            ByteBuffer bb = ByteBuffer.allocate(SIZE_OF_DOUBLE_IN_BYTES);
            bb.putDouble(to_serialize);
            bb.rewind();
            return ByteString.copyFrom(bb);
        }   
    }
    
    public static class AtomicNumber
        extends AtomicNumberVariable
        implements IHardwareStateSupplier<Double>, IHardwareChangeApplier<Double>
    {
        private final ExtendedHardwareOverrides<Double> extended_hardware_overrides;

        public AtomicNumber(
            RalphGlobals ralph_globals,
            VersionListener<Double> version_listener)
        {
            super(false,new Double(0),ralph_globals);
            
            extended_hardware_overrides =
                new ExtendedHardwareOverrides<Double>(
                    this,this,new SpeculateListener(),
                    version_listener,
                    false, // should not speculate
                    ralph_globals);
            extended_hardware_overrides.set_controlling_object(this);
        }

        /************** IHardwareChangeApplier overrides ************/
        @Override
        public boolean apply(Double to_apply)
        {
            return true;
        }
        @Override
        public boolean undo(Double to_undo)
        {
            return true;
        }
        

        /**************** IHardwareStateSupplier overrides *********/
        @Override
        public Double get_state_to_push(ActiveEvent active_event)
        {
            return this.dirty_val.val;
        }
        
        /**************** AtomicNumberVariable overrides *********/
        @Override
        protected ICancellableFuture hardware_first_phase_commit_hook(
            ActiveEvent active_event)
        {
            return extended_hardware_overrides.hardware_first_phase_commit_hook(
                active_event);
        }

        @Override
        protected void hardware_complete_commit_hook(ActiveEvent active_event)
        {
            extended_hardware_overrides.hardware_complete_commit_hook(active_event);
        }            

        @Override
        protected void hardware_backout_hook(ActiveEvent active_event)
        {
            extended_hardware_overrides.hardware_backout_hook(active_event);
        }

        @Override
        protected boolean hardware_first_phase_commit_speculative_hook(
            SpeculativeFuture sf)
        {
            return extended_hardware_overrides.hardware_first_phase_commit_speculative_hook(sf);
        }
    }
}
