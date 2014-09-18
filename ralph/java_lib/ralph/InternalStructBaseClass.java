package ralph;

import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;
import RalphVersions.ILocalVersionManager;

public abstract class InternalStructBaseClass implements IReference
{
    public final String uuid;
    public final RalphGlobals ralph_globals;

    public InternalStructBaseClass(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
        uuid = ralph_globals.generate_uuid();
    }
    
    public abstract ObjectContents serialize_contents(
        ActiveEvent active_event,Object add_contents);

    protected void log_obj_constructor_during_init(
        Object additional_serialization_contents)
    {
        ILocalVersionManager local_version_manager =
            VersioningInfo.instance.local_version_manager;
        if (local_version_manager != null)
        {
            // using null as active_event argument for
            // serialize_contents, gets internal value right away.
            ObjectContents obj_contents = 
                serialize_contents(
                    null,additional_serialization_contents);

            local_version_manager.save_object_constructor(
                uuid(), obj_contents);
        }
    }

    /** IReference overrides */
    
    @Override
    public String uuid()
    {
        return uuid;
    }
}