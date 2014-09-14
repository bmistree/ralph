package ralph;

import java.io.Serializable;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphDataWrappers.DataWrapper;
import RalphVersions.ILocalVersionManager;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;

public abstract class AtomicVariable<T,DeltaType>
    extends SpeculativeAtomicObject<T,DeltaType> 
{
    public AtomicVariable(
        boolean _log_changes, T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<DeltaType> version_helper,
        RalphGlobals ralph_globals)
    {
        super(ralph_globals);
        init_multithreaded_locked_object(
            vtdwc,version_helper,_log_changes,init_val);

        ILocalVersionManager local_version_manager =
            VersioningInfo.instance.local_version_manager;
        if (local_version_manager != null)
        {
            ObjectContents obj_contents = null;
            try
            {
                obj_contents = serialize_contents(null);
            }
            catch (BackoutException backout_exception)
            {
                backout_exception.printStackTrace();
                Util.logger_assert(
                    "Unexpected backout when constructing" +
                    "object: no other threads can access." );
            }
            // using null as active_event argument for
            // serialize_contents, gets internal value right away.
            local_version_manager.save_object_constructor(
                uuid(), obj_contents);
        }
    }

    @Override
    public void swap_internal_vals(
        ActiveEvent active_event,RalphObject to_swap_with)
        throws BackoutException
    {
        this.set_val(active_event,(T)to_swap_with.get_val(active_event));
    }
    
    @Override
    public boolean return_internal_val_from_container() 
    {
        return true;
    }
}
