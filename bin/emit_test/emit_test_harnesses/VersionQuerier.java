package emit_test_harnesses;

import com.google.protobuf.ByteString;

import java.nio.ByteBuffer;

import RalphConnObj.SingleSideConnection;

import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import RalphExtended.ISpeculateListener;
import RalphExtended.ExtendedHardwareOverrides;

import RalphVersions.VersionListener;
import RalphVersions.VersionManager;
import RalphVersions.IDeviceSpecificUpdateSerializer;

import ralph.RalphGlobals;
import ralph.Variables.AtomicNumberVariable;
import ralph.ICancellableFuture;
import ralph.ActiveEvent;
import ralph.SpeculativeFuture;

import ralph_emitted.AtomicNumberIncrementerJava.AtomicNumberIncrementer;
import ralph_emitted.AtomicNumberIncrementerJava._InternalWrappedNum;


public class VersionQuerier
{
    private final static RalphGlobals ralph_globals = new RalphGlobals();
    private final static int NUM_TIMES_TO_INCREMENT = 1000;
    
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

            _InternalWrappedNum internal_wrapped_num =
                generate_internal_wrapped_num();
            
            service.set_wrapped_num(internal_wrapped_num);

            for (int i = 0; i < NUM_TIMES_TO_INCREMENT; ++i)
                service.increment();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }

        return true;
    }


    public static _InternalWrappedNum generate_internal_wrapped_num()
    {
        VersionManager version_manager =
            new VersionManager(ralph_globals.clock);
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
