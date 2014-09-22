package ralph;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

import RalphVersions.ILocalVersionSaver;
import RalphVersions.ILocalVersionReplayer;
import RalphVersions.InMemoryLocalVersionManager;
import RalphVersions.DiskLocalVersionSaver;
import RalphVersions.DiskLocalVersionReplayer;


/**
   Singleton: keeps track of all the versioning data used by 
 */
public class VersioningInfo
{
    public static final VersioningInfo instance = new VersioningInfo();

    public final ILocalVersionSaver local_version_saver;
    public final ILocalVersionReplayer local_version_replayer;
    private VersioningInfo() 
    {
        Properties properties = new Properties();
        
        InputStream input_stream =
            getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input_stream == null)
        {
            local_version_saver = null;
            local_version_replayer = null;
        }
        else
        {
            try
            {
                properties.load(input_stream);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                System.exit (-1);
            }

            String which_versioner = (String)properties.get("versioner");
            if (which_versioner != null)
            {
                if (which_versioner.equals("in-memory"))
                {
                    InMemoryLocalVersionManager local_version_manager =
                        new InMemoryLocalVersionManager();
                    local_version_saver = local_version_manager;
                    local_version_replayer = local_version_manager;
                }
                else if (which_versioner.equals("disk"))
                {
                    Object obj_filename =
                        properties.get("disk-version-filename");
                    Object obj_buffer_capacity =
                        properties.get("disk-version-buffer-capacity");
                    if ((obj_filename == null) || (obj_buffer_capacity == null))
                    {
                        Util.logger_assert(
                            "Require 'disk-version-filename' in config and " +
                            "'disk-version-buffer-capacity' in config.");
                    }
                    // filename to use to save deltas to and read
                    // deltas from on replay.
                    String filename = (String) obj_filename;
                    // How many messages to allow to buffer before
                    // disk writes become blocking.
                    int buffer_capacity =
                        Integer.parseInt((String)obj_buffer_capacity);

                    local_version_saver = new DiskLocalVersionSaver(
                        buffer_capacity, filename);
                    local_version_replayer = new DiskLocalVersionReplayer(
                        filename);
                }
                //// DEBUG
                else
                {
                    Util.logger_assert(
                        "Unknown versioning type provided");
                    
                    // setting these here so that will compile without
                    // warning about how local variables may not have
                    // been initialized.
                    local_version_saver = null;
                    local_version_replayer = null;
                }
                //// END DEBUG
            }
            else
            {
                local_version_saver = null;
                local_version_replayer = null;
            }
        }
    }
}