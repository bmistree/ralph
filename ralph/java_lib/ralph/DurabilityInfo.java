package ralph;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import RalphDurability.IDurabilitySaver;
import RalphDurability.IDurabilityReplayer;
import RalphDurability.DiskDurabilitySaver;
import RalphDurability.NonSyncedWrites;
import RalphDurability.DurabilityReplayer;
import RalphDurability.DiskDurabilityReader;
import RalphDurability.ISerializedDurabilityReader;
import RalphDurability.SerializedDurabilityReader;
import RalphDurability.MultiDiskDurabilitySaver;


/**
   Singleton: keeps track of all the data used for durability.
 */
public class DurabilityInfo
{
    public static final DurabilityInfo instance = new DurabilityInfo();

    public final IDurabilitySaver durability_saver;
    public final IDurabilityReplayer durability_replayer;
    private DurabilityInfo() 
    {
        Properties properties = new Properties();
        
        InputStream input_stream =
            getClass().getClassLoader().getResourceAsStream("config.properties");
        if (input_stream == null)
        {
            durability_replayer = null;
            durability_saver = null;
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

            /******* LOADING DURABILITY SAVER *****/
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
                    durability_saver = new DiskDurabilitySaver(log_filename);
                }
                else if (which_durability.equals("multidisk"))
                {
                    Object obj_dirname = 
                        properties.get("disk-durability-dirname");
                    String dirname = (String) obj_dirname;

                    Object obj_num_synced_log_files =
                        properties.get("disk-durability-num-synced-log-files");
                    int num_synced_log_files = Integer.parseInt(
                        (String) obj_num_synced_log_files);

                    Object obj_num_non_synced_log_files =
                        properties.get(
                            "disk-durability-num-non-synced-log-files");
                    int num_non_synced_log_files = Integer.parseInt(
                        (String) obj_num_non_synced_log_files);

                    durability_saver =
                        new MultiDiskDurabilitySaver(
                            dirname,num_synced_log_files,
                            num_non_synced_log_files,
                            DiskDurabilitySaver.MANAGED_CONSTRUCTOR_FACTORY,
                            NonSyncedWrites.CONSTRUCTOR_FACTORY);
                }
                else if (which_durability.equals("lazy"))
                {
                    Object obj_dirname = 
                        properties.get("disk-durability-dirname");
                    String dirname = (String) obj_dirname;

                    Object obj_num_synced_log_files =
                        properties.get("disk-durability-num-synced-log-files");
                    int num_synced_log_files = Integer.parseInt(
                        (String) obj_num_synced_log_files);
                    
                    Object obj_num_non_synced_log_files =
                        properties.get(
                            "disk-durability-num-non-synced-log-files");
                    int num_non_synced_log_files = Integer.parseInt(
                        (String) obj_num_non_synced_log_files);
                    
                    durability_saver =
                        new MultiDiskDurabilitySaver(
                            dirname,num_synced_log_files,
                            num_non_synced_log_files,
                            NonSyncedWrites.CONSTRUCTOR_FACTORY,
                            NonSyncedWrites.CONSTRUCTOR_FACTORY);
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


            /******* LOADING DURABILITY REPLAYER *****/
            String which_durability_replayer =
                (String)properties.get("durability-replayer");
            if (which_durability_replayer != null)
            {
                if (which_durability_replayer.equals("disk"))
                {
                    Object obj_log_filename = 
                        properties.get(
                            "disk-durability-replayer-log-filename");
                    
                    // filename to use to save deltas to and read
                    // deltas from on replay.
                    String log_filename = (String) obj_log_filename;

                    DiskDurabilityReader durability_reader =
                        new DiskDurabilityReader(log_filename);

                    ISerializedDurabilityReader serialized_durability_reader =
                        new SerializedDurabilityReader(durability_reader);
                    
                    durability_replayer =
                        new DurabilityReplayer(serialized_durability_reader);
                    
                }
                //// DEBUG
                else
                {
                    Util.logger_assert(
                        "Unknown durability replayer type provided");
                    
                    // setting these here so that will compile without
                    // warning about how local variables may not have
                    // been initialized.
                    durability_replayer = null;
                }
                //// END DEBUG
            }
            else
                durability_replayer = null;
            
        }
    }
}