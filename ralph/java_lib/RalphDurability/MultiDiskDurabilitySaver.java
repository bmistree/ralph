package RalphDurability;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.File;

import ralph.EndpointConstructorObj;

public class MultiDiskDurabilitySaver implements IDurabilitySaver
{
    private final int num_log_files;
    private final List<DiskDurabilitySaver> list_of_dur_savers =
        new ArrayList<DiskDurabilitySaver>();
    private final AtomicInteger saver_index = new AtomicInteger(0);
    
    public MultiDiskDurabilitySaver(String dir_to_save_to,int num_log_files)
    {
        this.num_log_files = num_log_files;
        for (int i = 0; i <num_log_files; ++i)
        {
            String log_filename =
                dir_to_save_to + File.separator + "durability_log_" +
                i + ".bin";
            DiskDurabilitySaver dds = new DiskDurabilitySaver (log_filename);
            list_of_dur_savers.add(dds);
        }
    }

    @Override
    public void prepare_operation(IDurabilityContext dc)
    {
        int index = saver_index.addAndGet(1) % num_log_files;
        DiskDurabilitySaver dds = list_of_dur_savers.get(index);
        dds.prepare_operation(dc);
    }

    @Override
    public void ensure_logged_endpt_constructor(
        EndpointConstructorObj endpt_constructor_obj)
    {
        int index = saver_index.addAndGet(1) % num_log_files;
        DiskDurabilitySaver dds = list_of_dur_savers.get(index);
        dds.ensure_logged_endpt_constructor(endpt_constructor_obj);
    }
    
    @Override
    public void complete_operation(IDurabilityContext dc, boolean succeeded)
    {
        int index = saver_index.addAndGet(1) % num_log_files;
        DiskDurabilitySaver dds = list_of_dur_savers.get(index);
        dds.complete_operation(dc,succeeded);
    }
}