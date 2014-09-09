package RalphAtomicWrappers;
import ralph.RalphObject;
import ralph.Variables;
import ralph.RalphGlobals;

public class BaseAtomicWrappers
{
    public final static NonAtomicNumberWrapper NON_ATOMIC_NUMBER_WRAPPER =
        new NonAtomicNumberWrapper();
    public final static NonAtomicTrueFalseWrapper NON_ATOMIC_TRUE_FALSE_WRAPPER =
        new NonAtomicTrueFalseWrapper();
    public final static NonAtomicTextWrapper NON_ATOMIC_TEXT_WRAPPER =
        new NonAtomicTextWrapper();    

    public final static AtomicNumberWrapper ATOMIC_NUMBER_WRAPPER =
        new AtomicNumberWrapper();
    public final static AtomicTrueFalseWrapper ATOMIC_TRUE_FALSE_WRAPPER =
        new AtomicTrueFalseWrapper();
    public final static AtomicTextWrapper ATOMIC_TEXT_WRAPPER =
        new AtomicTextWrapper();    

    public static final String ATOMIC_TEXT_LABEL = "TVar Text";
    public static final String ATOMIC_TRUE_FALSE_LABEL = "TVar TrueFalse";
    public static final String ATOMIC_NUMBER_LABEL = "TVar Number";
    public static final String NON_ATOMIC_TEXT_LABEL = "Text";
    public static final String NON_ATOMIC_TRUE_FALSE_LABEL = "TrueFalse";
    public static final String NON_ATOMIC_NUMBER_LABEL = "Number";
    
    
    /**** Single threaded wrappers for base variables */
    private static class NonAtomicNumberWrapper
        implements EnsureAtomicWrapper<Double>
    {
        @Override
        public String get_serialization_label()
        {
            return NON_ATOMIC_NUMBER_LABEL;
        }
        
        @Override
        public RalphObject<Double> ensure_atomic_object(
            Double object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicNumberVariable(
                false,object_to_ensure,ralph_globals);
        }
    }

    private static class NonAtomicTrueFalseWrapper
        implements EnsureAtomicWrapper<Boolean>
    {
        @Override
        public String get_serialization_label()
        {
            return NON_ATOMIC_TRUE_FALSE_LABEL;
        }
        
        @Override
        public RalphObject<Boolean>ensure_atomic_object(
            Boolean object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicTrueFalseVariable(
                false,object_to_ensure,ralph_globals);
        }
    }
    
    private static class NonAtomicTextWrapper
        implements EnsureAtomicWrapper<String>
    {
        @Override
        public String get_serialization_label()
        {
            return NON_ATOMIC_TEXT_LABEL;
        }
        
        @Override
        public RalphObject<String>ensure_atomic_object(
            String object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicTextVariable(
                false,object_to_ensure,ralph_globals);
        }
    }

    /**** Multithreaded wrappers for base variables */
    private static class AtomicNumberWrapper
        implements EnsureAtomicWrapper<Double>
    {
        @Override
        public String get_serialization_label()
        {
            return ATOMIC_NUMBER_LABEL;
        }
        @Override
        public RalphObject<Double> ensure_atomic_object(
            Double object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.AtomicNumberVariable(
                false,object_to_ensure,ralph_globals);
        }
    }

    private static class AtomicTrueFalseWrapper
        implements EnsureAtomicWrapper<Boolean>
    {
        @Override
        public String get_serialization_label()
        {
            return ATOMIC_TRUE_FALSE_LABEL;
        }
        @Override
        public RalphObject<Boolean>ensure_atomic_object(
            Boolean object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.AtomicTrueFalseVariable(
                false,object_to_ensure,ralph_globals);
        }
    }
    
    private static class AtomicTextWrapper
        implements EnsureAtomicWrapper<String>
    {
        @Override
        public String get_serialization_label()
        {
            return ATOMIC_TEXT_LABEL;
        }
        @Override
        public RalphObject<String>ensure_atomic_object(
            String object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.AtomicTextVariable(
                false,object_to_ensure,ralph_globals);
        }
    }
}

