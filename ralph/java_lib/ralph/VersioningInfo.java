package ralph;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

import RalphVersions.IVersionSaver;
import RalphVersions.IVersionReplayer;
import RalphVersions.InMemoryVersionManager;
import RalphVersions.DiskVersionSaver;
import RalphVersions.DiskVersionReplayer;


/**
   Singleton: keeps track of all the versioning data used by 
 */
public class VersioningInfo
{
    public static final VersioningInfo instance = new VersioningInfo();

    public final IVersionSaver version_saver;
    public final IVersionReplayer version_replayer;
    private VersioningInfo() 
    {
        Properties properties = new Properties();
        
        InputStream input_stream =
            getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input_stream == null)
        {
            version_saver = null;
            version_replayer = null;
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
                    InMemoryVersionManager version_manager =
                        new InMemoryVersionManager();
                    version_saver = version_manager;
                    version_replayer = version_manager;
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

                    version_saver = new DiskVersionSaver(
                        buffer_capacity, filename);
                    version_replayer = new DiskVersionReplayer(
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
                    version_saver = null;
                    version_replayer = null;
                }
                //// END DEBUG
            }
            else
            {
                version_saver = null;
                version_replayer = null;
            }
        }
    }
}