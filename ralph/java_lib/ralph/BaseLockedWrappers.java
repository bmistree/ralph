package ralph;


public class BaseLockedWrappers
{
    public final static SingleThreadedNumberWrapper SINGLE_THREADED_NUMBER_WRAPPER =
        new SingleThreadedNumberWrapper();
    public final static SingleThreadedTrueFalseWrapper SINGLE_THREADED_TRUE_FALSE_WRAPPER =
        new SingleThreadedTrueFalseWrapper();
    public final static SingleThreadedTextWrapper SINGLE_THREADED_TEXT_WRAPPER =
        new SingleThreadedTextWrapper();    

    public final static NumberWrapper NUMBER_WRAPPER = new NumberWrapper();
    public final static TrueFalseWrapper TRUE_FALSE_WRAPPER = new TrueFalseWrapper();
    public final static TextWrapper TEXT_WRAPPER = new TextWrapper();    
    
    
    /**** Single threaded wrappers for base variables */
    public static class SingleThreadedNumberWrapper
        implements EnsureLockedWrapper<Double,Double>
    {
        public LockedObject<Double,Double> ensure_locked_object(
            Double object_to_ensure)
        {
            return new Variables.SingleThreadedLockedNumberVariable(
                "host_uuid",false,object_to_ensure);
        }
    }

    public static class SingleThreadedTrueFalseWrapper
        implements EnsureLockedWrapper<Boolean,Boolean>
    {
        public LockedObject<Boolean,Boolean>ensure_locked_object(
            Boolean object_to_ensure)
        {
            return new Variables.SingleThreadedLockedTrueFalseVariable(
                "host_uuid",false,object_to_ensure);
        }
    }
    
    public static class SingleThreadedTextWrapper
        implements EnsureLockedWrapper<String,String>
    {
        public LockedObject<String,String>ensure_locked_object(
            String object_to_ensure)
        {
            return new Variables.SingleThreadedLockedTextVariable(
                "host_uuid",false,object_to_ensure);
        }
    }

    /**** Multithreaded wrappers for base variables */
    public static class NumberWrapper
        implements EnsureLockedWrapper<Double,Double>
    {
        public LockedObject<Double,Double> ensure_locked_object(
            Double object_to_ensure)
        {
            return new Variables.LockedNumberVariable(
                "host_uuid",false,object_to_ensure);
        }
    }

    public static class TrueFalseWrapper
        implements EnsureLockedWrapper<Boolean,Boolean>
    {
        public LockedObject<Boolean,Boolean>ensure_locked_object(
            Boolean object_to_ensure)
        {
            return new Variables.LockedTrueFalseVariable(
                "host_uuid",false,object_to_ensure);
        }
    }
    
    public static class TextWrapper
        implements EnsureLockedWrapper<String,String>
    {
        public LockedObject<String,String>ensure_locked_object(
            String object_to_ensure)
        {
            return new Variables.LockedTextVariable(
                "host_uuid",false,object_to_ensure);
        }
    }
}

