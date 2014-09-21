package RalphVersions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import ralph.Util;
import ralph.EndpointConstructorObj;


public class DiskLocalVersionReplayer implements ILocalVersionReplayer
{
    protected final String filename;
    protected boolean has_been_initialized = false;
    protected InMemoryLocalVersionManager local_version_manager = null;
    
    public DiskLocalVersionReplayer (String _filename)
    {
        filename = _filename;
    }

    protected void init_local_version_manager()
    {
        local_version_manager = new InMemoryLocalVersionManager();
        try
        {
            File file = new File(filename);
            FileInputStream file_input_stream = new FileInputStream(file);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Error in disk local version replayer");
        }
        // VersionSaverMessages vsm = VersionSaverMessages.parseDelimitedFrom(
        //     file_input_stream);
    }

    @Override
    public EndpointInitializationHistory
        get_endpoint_initialization_history(String endpoint_uuid)
    {
        // FIXME: Fill in.
        Util.logger_assert(
            "Must fill in disk local version replayer get_endpt_init_history");
        return null;
    }

    /**
       @returns null if does not exist.
     */
    @Override
    public ObjectHistory get_full_object_history(String obj_uuid)
    {
        // FIXME: Fill in.
        Util.logger_assert(
            "Must fill in disk local version replayer get_full_object_history");
        return null;
    }

    /**
       @returns null if does not exist.
     */
    public EndpointConstructorObj get_endpoint_constructor_obj(
        String endpoint_constructor_obj_classname)
    {
        // FIXME: Fill in.
        Util.logger_assert(
            "Must fill in disk local version replayer get_endpt_constructor_obj");
        return null;
    }
    
    /**
       @param lower_range --- null if should query from earliest
       record.

       @param upper_range --- null if should query to latest record.
       
       @returns null if does not exist.  Returns objecthistory object
       with no records if no records exist within range.
     */
    public ObjectHistory get_ranged_object_history(
        String obj_uuid,Long lower_range, Long upper_range)
    {
        // FIXME: Fill in.
        Util.logger_assert(
            "Must fill in disk local version replayer get_ranged_object_history");
        return null;
    }
}