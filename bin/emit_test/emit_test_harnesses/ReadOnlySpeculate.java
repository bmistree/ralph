package emit_test_harnesses;

import ralph_emitted.ReadOnlySpeculateJava.ReadOnlySpeculateService;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicBoolean;

/**
  Trying to get case where:
   
    evt reads a speculated version of an obj, obj'.
    Then obj' is promoted to root obj.
    Then evt reads obj.
    Then evt tries to commit.

  Concern is that when we promote obj' to obj, we will get two
  complete commit requests (one forwarded from obj' to obj and one
  directly to obj).  If obj has already cleared the read from its
  read_lock_holders, then can get into bad shape because forwarded
  commit request gets rolled back.
 */
public class ReadOnlySpeculate
{
    final public static AtomicBoolean had_exception = new AtomicBoolean(false);
    final public static AtomicBoolean succeeded = new AtomicBoolean(false);
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ReadOnlySpeculate\n");
        else
            System.out.println("\nFAILURE in ReadOnlySpeculate\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            final ReadOnlySpeculateService endpt =
                new ReadOnlySpeculateService(
                    new RalphGlobals(),new SingleSideConnection());

            Thread speculater_thread = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        endpt.speculater();
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        had_exception.set(true);
                    }
                }
            };

            Thread double_commit_thread = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        boolean completed_no_backout = endpt.double_commit_test();
                        succeeded.set(completed_no_backout);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        had_exception.set(true);
                    }
                }
            };
            

            speculater_thread.start();
            Thread.sleep(30);
            double_commit_thread.start();
            speculater_thread.join();
            double_commit_thread.join();

            if (had_exception.get())
                return false;
            
            return succeeded.get();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
    }
}
