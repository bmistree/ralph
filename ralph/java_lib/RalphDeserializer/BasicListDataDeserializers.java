package RalphDeserializer;

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
import static RalphDeserializer.Deserializer.dummy_deserialization_active_event;
import RalphAtomicWrappers.EnsureAtomicWrapper;

public class BasicListDataDeserializers
{
    // only used to force populating internal static fields
    private final static BasicListDataDeserializers instance =
        new BasicListDataDeserializers();
    protected BasicListDataDeserializers()
    {}
    public static BasicListDataDeserializers get_instance()
    {
        return instance;
    }

    // Note: Atomic lists still have non-atomic internal elements.
    private final static AtomListDeserializer<Double> atom_num_list_constructor =
        new AtomListDeserializer<Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);
    private final static AtomListDeserializer<String> atom_text_list_constructor =
        new AtomListDeserializer<String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER);
    private final static AtomListDeserializer<Boolean> atom_tf_list_constructor =
        new AtomListDeserializer<Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER);
    // non-atomic list constructors
    private final static NonAtomListDeserializer<Double> non_atom_num_list_constructor =
        new NonAtomListDeserializer<Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);
    private final static NonAtomListDeserializer<String> non_atom_text_list_constructor =
        new NonAtomListDeserializer<String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER);
    private final static NonAtomListDeserializer<Boolean> non_atom_tf_list_constructor =
        new NonAtomListDeserializer<Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER);

    public static class AtomListDeserializer<ElementType>
        implements DataDeserializer
    {
        private final EnsureAtomicWrapper wrapper;
        
        public AtomListDeserializer(String element_label,EnsureAtomicWrapper _wrapper)
        {
            wrapper = _wrapper;
            Deserializer deserializer =
                Deserializer.get_instance();
            
            String label = deserializer.merge_labels(
                AtomicList.deserialization_label,
                element_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject deserialize(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicListVariable<ElementType,ElementType> outer_list =
                new AtomicListVariable<ElementType,ElementType>(
                    false,wrapper,ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || (! any.getIsTvar()))
                Util.logger_assert("Cannot deserialize list without list field");
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            Deserializer deserializer =
                Deserializer.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    RalphObject non_atom_element =
                        deserializer.deserialize(list_element,ralph_globals);
                    non_atom_element.get_val(evt);
                    outer_list.get_val(evt).append(
                        evt,
                        (ElementType)non_atom_element.get_val(evt));
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }

            try
            {
                to_return = outer_list.get_val(null);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Should never be backed out when deserializing");
            }
            
            // return internal list
            return to_return;
        }
    }
    

    public static class NonAtomListDeserializer<ElementType>
        implements DataDeserializer
    {
        private final EnsureAtomicWrapper wrapper;
        
        public NonAtomListDeserializer(String element_label,EnsureAtomicWrapper _wrapper)
        {
            wrapper = _wrapper;
            Deserializer deserializer =
                Deserializer.get_instance();
            
            String label = deserializer.merge_labels(
                NonAtomicList.deserialization_label,
                element_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject deserialize(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicListVariable<ElementType,ElementType> outer_list =
                new NonAtomicListVariable<ElementType,ElementType>(
                    false,wrapper,ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || ( any.getIsTvar()))
                Util.logger_assert("Cannot deserialize list without list field");
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            Deserializer deserializer =
                Deserializer.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    RalphObject non_atom_element =
                        deserializer.deserialize(list_element,ralph_globals);
                    non_atom_element.get_val(evt);
                    outer_list.get_val(evt).append(
                        evt,
                        (ElementType)non_atom_element.get_val(evt));
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }

            try
            {
                to_return = outer_list.get_val(null);
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                Util.logger_assert(
                    "Should never be backed out when deserializing");
            }

            
            // return internal list
            return to_return;
        }
    }
}