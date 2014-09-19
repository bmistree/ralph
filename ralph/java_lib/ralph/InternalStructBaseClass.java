package ralph;

import java.util.List;

import RalphExceptions.BackoutException;
import ralph_local_version_protobuffs.ObjectContentsProto.ObjectContents;
import RalphVersions.ILocalVersionSaver;


public abstract class InternalStructBaseClass
    extends DummyRalphObject<Object,Object>
{
    public final String uuid;
    public final RalphGlobals ralph_globals;

    // Elements are internal uuids of fields.  Should be ordered
    // alphabetically by field name.  Ie, for struct:
    //
    // Struct SomeStruct
    // {
    //     Number c;
    //     Number a;
    // }
    // first entry should be the uuid of the RalphObject holding
    // Number a.
    protected List<String> internal_references_to_replay_on = null;

    
    public InternalStructBaseClass(RalphGlobals ralph_globals)
    {
        this.ralph_globals = ralph_globals;
        uuid = ralph_globals.generate_uuid();
    }

    public void set_internal_references_to_replay_on(
        List<String> to_set_to)
    {
        internal_references_to_replay_on = to_set_to;
    }
    
    protected void log_obj_constructor_during_init(
        Object additional_serialization_contents)
    {
        ILocalVersionSaver local_version_saver =
            VersioningInfo.instance.local_version_saver;
        if (local_version_saver != null)
        {
            // using null as active_event argument for
            // serialize_contents, gets internal value right away.
            ObjectContents obj_contents = null;
            try
            {
                obj_contents = serialize_contents(
                    null,additional_serialization_contents);
            }
            catch(BackoutException ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Unexpected backout when initially " +
                    "serializing internal struct.");
            }

            local_version_saver.save_object_constructor(
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