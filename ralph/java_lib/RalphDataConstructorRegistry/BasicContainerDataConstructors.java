package RalphDataConstructorRegistry;

import java.util.List;

import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.AtomicList;
import ralph.NonAtomicList;
import ralph_protobuffs.VariablesProto;
import static ralph.Variables.AtomicListVariable;
import static ralph.Variables.NonAtomicListVariable;
import static ralph.Variables.NonAtomicNumberVariable;
import static ralph.Variables.NonAtomicTextVariable;
import static ralph.Variables.NonAtomicTrueFalseVariable;
import RalphAtomicWrappers.BaseAtomicWrappers;
import ralph.Util;
import ralph.ActiveEvent;


public class BasicContainerDataConstructors
{
    // only used to force populating internal static fields
    private final static BasicContainerDataConstructors instance =
        new BasicContainerDataConstructors();
    protected BasicContainerDataConstructors()
    {}
    public static BasicContainerDataConstructors get_instance()
    {
        return instance;
    }

    // reuse same event when deserializing
    private final static DeserializationEvent const_deserialization_event =
        new DeserializationEvent();
    protected static ActiveEvent dummy_deserialization_active_event()
    {
        return const_deserialization_event;
    }

    
    /** List deserializers*/
    private final static AtomNumListConstructor dummy_atom_num_list_constructor =
        AtomNumListConstructor.get_instance();
    private static class AtomNumListConstructor implements DataConstructor
    {
        private final static AtomNumListConstructor singleton =
            new AtomNumListConstructor();
        
        protected AtomNumListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                AtomicList.deserialization_label,
                BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL);

            deserializer.register(label,this);
        }
        public static AtomNumListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicListVariable<Double,Double> outer_list =
                new AtomicListVariable<Double,Double>(
                    false,BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
                    ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || (! any.getIsTvar()))
                Util.logger_assert("Cannot deserialize list without list field");
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicNumberVariable non_atom_number =
                        (NonAtomicNumberVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    outer_list.get_val(evt).append(evt,non_atom_number.get_val(evt));
                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list
            return to_return;
        }
    }


    private final static NonAtomNumListConstructor dummy_non_atom_num_list_constructor =
        NonAtomNumListConstructor.get_instance();
    private static class NonAtomNumListConstructor implements DataConstructor
    {
        private final static NonAtomNumListConstructor singleton =
            new NonAtomNumListConstructor();
        
        protected NonAtomNumListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                NonAtomicList.deserialization_label,
                BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL);

            deserializer.register(label,this);
        }
        public static NonAtomNumListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicListVariable<Double,Double> outer_list =
                new NonAtomicListVariable<Double,Double>(
                    false,BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
                    ralph_globals);
            RalphObject to_return = null;

            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || any.getIsTvar())
            {
                Util.logger_assert(
                    "Incorrectly deserializing as non-atomic list.");
            }
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicNumberVariable non_atom_number =
                        (NonAtomicNumberVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    outer_list.get_val(evt).append(evt,non_atom_number.get_val(evt));
                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list            
            return to_return;
        }
    }

    /** Atomic text list*/
    private final static AtomTextListConstructor dummy_atom_text_list_constructor =
        AtomTextListConstructor.get_instance();
    private static class AtomTextListConstructor implements DataConstructor
    {
        private final static AtomTextListConstructor singleton =
            new AtomTextListConstructor();
        
        protected AtomTextListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                AtomicList.deserialization_label,
                BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL);

            deserializer.register(label,this);
        }
        public static AtomTextListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicListVariable<String,String> outer_list =
                new AtomicListVariable<String,String>(
                    false,BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
                    ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || (! any.getIsTvar()))
                Util.logger_assert("Cannot deserialize list without list field");
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicTextVariable atom_text =
                        (NonAtomicTextVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    outer_list.get_val(evt).append(evt,atom_text.get_val(evt));
                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list
            return to_return;
        }
    }

    
    /** NonAtomic Text list*/
    private final static NonAtomTextListConstructor dummy_non_atom_text_list_constructor =
        NonAtomTextListConstructor.get_instance();
    private static class NonAtomTextListConstructor implements DataConstructor
    {
        private final static NonAtomTextListConstructor singleton =
            new NonAtomTextListConstructor();
        
        protected NonAtomTextListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                NonAtomicList.deserialization_label,
                BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL);

            deserializer.register(label,this);
        }
        public static NonAtomTextListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicListVariable<String,String> outer_list =
                new NonAtomicListVariable<String,String>(
                    false,BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER,
                    ralph_globals);
            RalphObject to_return = null;

            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || any.getIsTvar())
            {
                Util.logger_assert(
                    "Incorrectly deserializing as non-atomic list.");
            }
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicTextVariable non_atom_text =
                        (NonAtomicTextVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    outer_list.get_val(evt).append(evt,non_atom_text.get_val(evt));
                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list            
            return to_return;
        }
    }

    /** Atomic truefalse list*/
    private final static AtomTrueFalseListConstructor dummy_atom_tf_list_constructor =
        AtomTrueFalseListConstructor.get_instance();
    private static class AtomTrueFalseListConstructor implements DataConstructor
    {
        private final static AtomTrueFalseListConstructor singleton =
            new AtomTrueFalseListConstructor();
        
        protected AtomTrueFalseListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                AtomicList.deserialization_label,
                BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL);

            deserializer.register(label,this);
        }
        public static AtomTrueFalseListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicListVariable<Boolean,Boolean> outer_list =
                new AtomicListVariable<Boolean,Boolean>(
                    false,BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
                    ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || (! any.getIsTvar()))
                Util.logger_assert("Cannot deserialize list without list field");
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicTrueFalseVariable atom_tf =
                        (NonAtomicTrueFalseVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    outer_list.get_val(evt).append(evt,atom_tf.get_val(evt));
                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list
            return to_return;
        }
    }

    
    /** NonAtomic TrueFalse list*/
    private final static NonAtomTrueFalseListConstructor dummy_non_atom_tf_list_constructor =
        NonAtomTrueFalseListConstructor.get_instance();
    private static class NonAtomTrueFalseListConstructor implements DataConstructor
    {
        private final static NonAtomTrueFalseListConstructor singleton =
            new NonAtomTrueFalseListConstructor();
        
        protected NonAtomTrueFalseListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                NonAtomicList.deserialization_label,
                BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL);

            deserializer.register(label,this);
        }
        public static NonAtomTrueFalseListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicListVariable<Boolean,Boolean> outer_list =
                new NonAtomicListVariable<Boolean,Boolean>(
                    false,BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER,
                    ralph_globals);
            RalphObject to_return = null;

            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || any.getIsTvar())
            {
                Util.logger_assert(
                    "Incorrectly deserializing as non-atomic list.");
            }
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicTrueFalseVariable non_atom_tf =
                        (NonAtomicTrueFalseVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    outer_list.get_val(evt).append(evt,non_atom_tf.get_val(evt));
                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list            
            return to_return;
        }
    }
}