package ralph;

import java.io.Serializable;

import RalphExceptions.BackoutException;
import RalphDataWrappers.ValueTypeDataWrapperFactory;
import RalphDataWrappers.ValueTypeDataWrapper;
import RalphVersions.IVersionSaver;

import ralph_protobuffs.ObjectContentsProto.ObjectContents;

public abstract class NonAtomicVariable<T,DeltaType>
    extends NonAtomicObject<T,DeltaType>
{
    public NonAtomicVariable(
        T init_val,
        ValueTypeDataWrapperFactory<T> vtdwc,
        VersionHelper<DeltaType> version_helper,
        RalphGlobals ralph_globals,
        Object additional_serialization_contents)
    {
        super (ralph_globals);
        init(vtdwc,init_val, version_helper);

        IVersionSaver version_saver =
            VersioningInfo.instance.version_saver;
        if (version_saver != null)
        {
            ObjectContents obj_contents = null;
            try
            {
                obj_contents = serialize_contents(
                    null,additional_serialization_contents,null);
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
            version_saver.save_object_constructor(
                uuid(), obj_contents);
        }
    }

    @Override
    public boolean return_internal_val_from_container()
    {
        return true;
    }
}
