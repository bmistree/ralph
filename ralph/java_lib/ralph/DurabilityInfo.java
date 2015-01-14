package ralph;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import RalphDurability.IDurabilitySaver;


/**
   Singleton: keeps track of all the data used for durability.
 */
public class DurabilityInfo
{
    public static final DurabilityInfo instance = new DurabilityInfo();

    public final IDurabilitySaver durability_saver;
    private DurabilityInfo() 
    {
        Properties properties = new Properties();
        
        InputStream input_stream =
            getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input_stream == null)
            durability_saver = null;
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

            String which_durability = (String)properties.get("durability");
            if (which_durability != null)
            {
                if (which_durability.equals("disk"))
                {
                    Object obj_log_filename = 
                        properties.get("disk-durability-log-filename");
                    
                    // filename to use to save deltas to and read
                    // deltas from on replay.
                    String log_filename = (String) obj_log_filename;

                    // FIXME: actually create a disk-based durability
                    // saver.
                    durability_saver = null;
                    
                }
                //// DEBUG
                else
                {
                    Util.logger_assert("Unknown durability type provided");
                    
                    // setting these here so that will compile without
                    // warning about how local variables may not have
                    // been initialized.
                    durability_saver = null;
                }
                //// END DEBUG
            }
            else
                durability_saver = null;
        }
    }
}