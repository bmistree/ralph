import ralph.parse.ast_labels as ast_labels
from ralph.java_emit.emit_utils import indent_string
from ralph.java_emit.emit_context import EmitContext
from ralph.parse.type import BasicType,MethodType,MapType
from ralph.parse.ast_labels import BOOL_TYPE, NUMBER_TYPE, STRING_TYPE
from ralph.java_emit.emit_utils import InternalEmitException

def emit(root_node,package_name,program_name):
    '''
    @param {RootStatementNode} root_node 
    
    @returns {String} --- Emitted program text
    '''

    prog_txt = '''
package %s;

import ralph.*;
import ralph.LockedVariables.LockedNumberVariable;
import ralph.LockedVariables.LockedTextVariable;
import ralph.LockedVariables.LockedTrueFalseVariable;
import ralph.LockedVariables.SingleThreadedLockedNumberVariable;
import ralph.LockedVariables.SingleThreadedLockedTextVariable;
import ralph.LockedVariables.SingleThreadedLockedTrueFalseVariable;
import ralph.LockedVariables.SingleThreadedMapVariable;
import ralph.LockedVariables.MultiThreadedMapVariable;
// index types for maps
import ralph.SingleThreadedLockedContainer.IndexType;

import RalphConnObj.ConnectionObj;
import RalphExceptions.*;
import java.util.Arrays;
import java.util.ArrayList;
import ralph.Util;
import RalphCallResults.EndpointCallResultObject;
import java.util.concurrent.ArrayBlockingQueue;
import RalphCallResults.EndpointCompleteCallResult;
import java.util.HashMap;


public class %s
{
''' % (package_name,program_name)

    for endpt_node in root_node.endpoint_node_list:
        prog_txt += indent_string(emit_endpt(endpt_node))
        prog_txt += '\n'

    prog_txt += '}' # closes class program_name

    return prog_txt
        
def emit_endpt(endpt_node):
    '''
    @param {EndpointDefinitionNode} endpt_node

    @returns {String}
    '''
    emit_ctx = EmitContext()
    emit_ctx.push_scope()    
    endpt_class_signature = (
        'public static class %s extends Endpoint { \n' % endpt_node.name)
    
    endpt_class_body = emit_endpt_variable_declarations(
        emit_ctx,endpt_node.body_node.variable_declaration_nodes)
    endpt_class_body += '\n'
    endpt_class_body += emit_constructor(emit_ctx,endpt_node)
    endpt_class_body += emit_endpt_method_declarations(
        emit_ctx,endpt_node.body_node.method_declaration_nodes)
    endpt_class_body += '\n'
    endpt_class_body += emit_rpc_dispatch(
        emit_ctx,endpt_node.body_node.method_declaration_nodes)
    endpt_class_body += '\n'
    return endpt_class_signature + indent_string(endpt_class_body) + '\n}'



def emit_constructor(emit_ctx,endpt_node):
    constructor_text = '''
public %s ( RalphGlobals ralph_globals,String host_uuid,
            ConnectionObj conn_obj) throws Exception {
    super(ralph_globals,host_uuid,conn_obj,new VariableStore(false));
}
''' % endpt_node.name
    return constructor_text

def emit_endpt_variable_declarations(emit_ctx,variable_declaration_node_list):
    '''
    @param {list} variable_declaration_node_list --- Each element is
    a DeclarationStatementNode.
    '''
    emit_ctx.set_in_endpoint_global_vars(True)
    endpoint_variable_text = ''
    for variable_declaration_node in variable_declaration_node_list:
        endpoint_variable_text += (
            emit_statement(emit_ctx,variable_declaration_node) )
        
    emit_ctx.set_in_endpoint_global_vars(False)
    return endpoint_variable_text

def convert_args_text_for_dispatch(method_declaration_node):
    '''
    When emitting rpc dispatch method for each endpoint, must convert
    args passed in to method into args that the method will use.

    This method returns a text string that contains all the argument
    casts necessary to call internal methods.  Eg.,

    Double arg0 = ((LockedObject<Double,Double>)args[0]).get_val(null);

    Note that it is dumb that we deserialize into a locked object,
    instead of the value directly.  Should change.

    @param {int} index --- Which value of args array to read from and
    how to name transalted arg (eg., "arg0" above instead of "arg1" or
    "arg2".)
    '''
    print (
        'FIXME: when deserialize should deserialize into ' +
        'a java native object instead of a locked object.')

    method_declaration_arg_node_list = (
        method_declaration_node.method_signature_node.method_declaration_args)

    to_return = ''
    for arg_number in range(0,len(method_declaration_arg_node_list)):
        method_declaration_arg_node = method_declaration_arg_node_list[arg_number]
        arg_name = 'arg%i' % arg_number
        arg_vec_to_read_from = 'args[%i]' % arg_number

        if isinstance(method_declaration_arg_node.type, MapType):
            # for map types, just push the map variable directly as
            # argument to method.  (Do not need to call get_val
            # directly because when pass arguments to a method, the
            # method expects a map type.
            map_type = emit_internal_type(method_declaration_arg_node.type)
            single_arg_string = (
                '%s %s = (%s) %s;' %
                (map_type, arg_name, map_type, arg_vec_to_read_from))
        else:
            locked_type,java_type = get_method_arg_type_as_locked(
            method_declaration_arg_node)

            single_arg_string = '''
%s %s = ((%s)%s).get_val(null);
''' % (java_type,arg_name,locked_type,arg_vec_to_read_from)
            
        to_return += single_arg_string
    return to_return

def get_method_arg_type_as_locked(method_declaration_arg_node):
    '''
    Takes in a method's declared argument ast node and returns a
    string for a LockedObject of that type.  Eg., if
    method_declaration_arg_node has type Number, return
      LockedObject<Double,Double>, Double
    String, return:
      LockedObject<String,String>, String
    Boolean, return:
      LockedObject<Boolean,Boolean>, Boolean
    '''
    if isinstance(method_declaration_arg_node.type,BasicType):
        arg_basic_type = method_declaration_arg_node.type.basic_type
        if arg_basic_type == BOOL_TYPE:
            java_type = 'Boolean'
        elif arg_basic_type == NUMBER_TYPE:
            java_type = 'Double'
        elif arg_basic_type == STRING_TYPE:
            java_type = 'String'
        #### DEBUG
        else:
            raise InternalEmitException(
                'Unknown basic type when emitting.')
        #### END DEBUG
        return ('LockedObject<%s,%s>' % (java_type,java_type)), java_type

    #### DEBUG
    else:
        raise InternalEmitException('Unknown argument type for method.')
    #### END DEBUG
        

def exec_dispatch_sequence_call(method_declaration_node):
    '''
    When emitting rpc dispatch method, this method actually executes
    method call associated with method_declaration_node, assuming that
    convert_args_text_for_dispatch has run and put the method's
    arugments into arg0, arg1, arg2, etc.
    '''
    method_name = method_declaration_node.method_name
    method_declaration_arg_node_list = (
        method_declaration_node.method_signature_node.method_declaration_args)

    # assumes that all arguments are named arg0, arg1, arg2,
    # etc. (should be based on running convert_args_text_for_dispatch.
    arg_list = map(
        lambda arg_number:
            'arg%i'%arg_number,
        range(0,len(method_declaration_arg_node_list)))
    
    args_text = ','.join(arg_list)
    if args_text != '':
        args_text = ',' + args_text
    
    actual_call = method_name + '(ctx,active_event' + args_text + ');\n'
    if method_declaration_node.returns_value():
        actual_call = 'result = (Object)' + actual_call
    return actual_call


def emit_rpc_dispatch(emit_ctx,method_declaration_node_list):
    '''
    Each endpoint has a method for dispatching rpcs that takes in the
    name of the method to execute locally as well as Object arguments
    to that method.  This method then actually executes the local
    method associated with the rpc call.
    '''
    # contains actual if-else if-else logic for dispatch method.
    rpc_text_for_methods = ''
    first = True
    for index in range(0,len(method_declaration_node_list)):
        method_declaration_node = method_declaration_node_list[index]
        # trying to add something like:
        # else if (to_exec_internal_name.equals("test_partner_args_method"))
        # {
        #     LockedObject<Double,Double> num_obj =
        #         (LockedObject<Double,Double>)args[0];
        #     _test_partner_args_method(active_event, ctx, num_obj);
        #     ctx.hide_sequence_completed_call(this, active_event);
        # }
        # to method body.

        # whether is if or else if
        if_elif = 'else if'
        if first:
            first = False
            if_elif = 'if'

        # contents of if-elif statement
        if_elif_body = (
            convert_args_text_for_dispatch(method_declaration_node) +
            exec_dispatch_sequence_call(method_declaration_node))

        rpc_text_for_methods += (
            if_elif + '(to_exec_internal_name.equals("' +
            method_declaration_node.method_name + '")) {\n' + 
            indent_string(if_elif_body,1) + 
            '\n}\n')
    
    #### For debugging: what happens if asked to run an rpc method
    #### that isn't available locally.
    if rpc_text_for_methods != '':
        rpc_text_for_methods += '''
else
{
    Util.logger_assert(
        "Error handling rpc call: unknown method " +
        to_exec_internal_name);
}

'''
    #### End debug
    
    emitted_method = '''
protected void _handle_rpc_call(
    String to_exec_internal_name,ActiveEvent active_event,
    ExecutingEventContext ctx,
    ArrayBlockingQueue<EndpointCallResultObject> result_queue,
    Object...args)
    throws ApplicationException, BackoutException, NetworkException,
    StoppedException
{
    Object result = null;

%s

    if (result_queue == null)
        return;

    boolean completed = active_event.wait_if_modified_peered();
    if (! completed)
    {
        result_queue.add(
            new RalphCallResults.BackoutBeforeEndpointCallResult());
    }
    else
    {
        result_queue.add(new EndpointCompleteCallResult(result));
    }
}
''' % indent_string(rpc_text_for_methods,1)

    return emitted_method


def emit_endpt_method_declarations(emit_ctx,method_declaration_node_list):
    '''
    @param {list} method_declaration_node_list --- Each element is a
    MethodDeclarationNode.
    '''
    to_return = ''

    # first, for each node, load the method name into emit_ctx so can
    # reference it later.
    for method_declaration_node in method_declaration_node_list:
        add_method_signature_to_ctx(emit_ctx,method_declaration_node)
    
    for method_declaration_node in method_declaration_node_list:
        to_return += emit_method_declaration_node(
            emit_ctx,method_declaration_node)
        to_return += '\n'

    return to_return

def add_method_signature_to_ctx(emit_ctx,method_declaration_node):
    """Add method signature to emit_ctx so that can reference it
    later in lookup to emit_ctx
    """
    method_signature_node = method_declaration_node.method_signature_node
    emit_ctx.add_method_name_to_method_set(method_signature_node.method_name)

def emit_method_declaration_node(emit_ctx,method_declaration_node):
    '''
    @param {MethodDeclarationNode} method_declaration_node
    '''
    external_method_text = emit_external_facing_method(
        emit_ctx,method_declaration_node.method_signature_node)
    
    emit_ctx.push_scope()
    signature_plus_head = emit_method_signature_plus_head(
        emit_ctx,
        method_declaration_node.method_signature_node)

    body = ''
    for statement in method_declaration_node.method_body_statement_list:
        body += emit_statement(emit_ctx,statement)
        body += ';\n'
        
    emit_ctx.pop_scope()

    # all return statements already pop from var stacks themselves.
    # this is because java compiler throws error for unreachable code.
    # Eg.,
    #
    #      _ctx.var_stack.push(true);//true because func scope
    #      return __internal__0internal_number.get_val(_active_event);
    #      _ctx.var_stack.pop();
    #
    # will throw a compile error becaue could never hit _ctx.var_stack.pop();
    if method_declaration_node.method_signature_node.type.returns_type is None:
        body += '_ctx.var_stack.pop();'
    internal_method_text = signature_plus_head + indent_string(body) + '\n}'
    
    return external_method_text + internal_method_text


def emit_external_facing_method(emit_ctx,method_signature_node):
    """Methods can be called from both code within ralph and code
    external to ralph.  The external facing code requires different
    arguments and just calls into the internal code.
    """
    # creating method signature
    return_type = 'void'
    void_return_type = True

    if method_signature_node.type.returns_type is not None:
        return_type = emit_internal_type(method_signature_node.type)
        void_return_type = False
        
    to_return = (
        'public %s %s (' % (return_type, method_signature_node.method_name) )
    argument_text_list = []
    argument_name_text_list = []
    for argument_node in method_signature_node.method_declaration_args:
        # for placing the arguments actually in method signature
        argument_type_text = emit_internal_type(argument_node.type)
        argument_name_text = argument_node.arg_name
        argument_text_list.append(
            argument_type_text + ' ' + argument_name_text)

        # for putting arguments into internal function call
        argument_name_text_list.append(argument_name_text)

    # finish method signature
    to_return += ','.join(argument_text_list) + ') throws Exception {\n'

    # call the internal version of the function
    method_body_text = 'ExecutingEventContext ctx = create_context();\n'
    method_body_text += '''
ActiveEvent active_event = _act_event_map.create_root_non_atomic_event();
'''
    
    inner_method_call_text = (
        '%s (ctx ,active_event' % method_signature_node.method_name)
    for argument_name in argument_name_text_list:
        inner_method_call_text += ',' + argument_name
    inner_method_call_text += ');\n'

    if not void_return_type:
        # assign the method call to an object to return
        inner_method_call_text = (
            '%s to_return = %s' % (return_type, inner_method_call_text))

    method_body_text += inner_method_call_text
        
    # try to commit the event
    method_body_text += '''
active_event.begin_first_phase_commit();
try {
    ((RootEventParent)active_event.event_parent).event_complete_queue.take();
} catch (InterruptedException _ex) {
    // TODO Auto-generated catch block
    _ex.printStackTrace();
}
'''

    # return the grabbed value
    if not void_return_type:
        method_body_text += 'return to_return;'
        
    to_return += indent_string(method_body_text) + '\n}\n' 
    return to_return


def emit_method_signature_plus_head(emit_ctx,method_signature_node):
    '''
    @param {EmitContext} emit_ctx --- Loads arguments to method into
    emit_ctx.

    @param {MethodSignatureNode} method_signature_node ---

    @returns {String} --- A java signature for method.  Eg.,

    private Double some_method (
        ExecutingEventContext _ctx, LockedActiveEvent _active_event,
        SomeType SomeVar) 
        throws ApplicationException, BackoutException, NetworkException,
        StoppedException
    {
        _ctx.var_stack.push(true); // true because function var scope
        _ctx.var_stack.add_var('SomeVar',SomeVar);
    '''
    # 1: update context with loaded arguments
    for argument_node in method_signature_node.method_declaration_args:
        arg_name = argument_node.arg_name
        emit_ctx.add_var_name(arg_name)

    # 2: construct signature to return
    return_type = 'void'
    if method_signature_node.type is not None:
        return_type = emit_internal_type(method_signature_node.type)

    to_return = (
        'private %s %s (' % (return_type, method_signature_node.method_name) )
    to_return += 'ExecutingEventContext _ctx, ActiveEvent _active_event'
    
    argument_name_text_list = []
    for argument_node in method_signature_node.method_declaration_args:
        # for placing the arguments actually in method signature
        argument_type_text = emit_internal_type(argument_node.type)
        argument_name_text = argument_node.arg_name
        to_return += ', ' + argument_type_text + ' ' + argument_name_text

        # for putting arguments into scope at top of method
        argument_name_text_list.append(argument_name_text)


    to_return += (
        ') throws ApplicationException, BackoutException, ' +
        'NetworkException,StoppedException {\n')
    # 3: emit head section where add to scope stack and push arguments
    # on to scope stack.  Must push arguments on to scope stack so
    # they're available in defer statements
    to_return += indent_string(
        '_ctx.var_stack.push(true);//true because func scope\n');

    # convert each method argument to ralph variable and then add to
    # stack.  Note: skip this step for maps, which caller already
    # cloned for us.
    for index in range(0,len(argument_name_text_list)):
        argument_name = argument_name_text_list[index]
        argument_node = method_signature_node.method_declaration_args[index]
        argument_type = argument_node.type

        if isinstance(argument_type,MapType):
            # reference types are cloned by callers: do not need to
            # wrap their java versions in Ralph objects.  Just need to
            # add the variable to the context stack.
            emit_ctx.set_var_name(argument_name)
            to_return += indent_string(
                '_ctx.var_stack.add_var("%s",%s);\n' %
                (argument_name, argument_name))
            continue

        java_type_statement = emit_ralph_wrapped_type(argument_type,True)
                
        new_ralph_variable = (
            'new %s (_host_uuid,false,%s)' %
            (java_type_statement,argument_name))

        internal_arg_name = emit_ctx.lookup_internal_var_name(argument_name)
        to_return +=indent_string(
            '%s %s = %s;\n' %
            (java_type_statement,internal_arg_name,new_ralph_variable))
        to_return += indent_string(
            '\n_ctx.var_stack.add_var("%s",%s);\n' %
            (internal_arg_name,internal_arg_name))

    return to_return


def emit_ralph_wrapped_type(type_object,force_single_threaded=False):
    '''
    @param {Type or None} type_object --- None if type corresponds to
    void (eg., in method signature).

    @returns{String} --- Java-ized version of wrapped Ralph type: eg.,
    LockedNumberVarialbe, etc.
    '''    
    if type_object is None:
        return 'void'
    else:
        # emit for maps
        if isinstance(type_object,MapType):
            return emit_map_type(type_object)
        
        # emit for others
        if isinstance(type_object,BasicType):
            typer = type_object.basic_type
            is_tvar = type_object.is_tvar
        elif isinstance(type_object,MethodType):
            typer = type_object.returns_type
            is_tvar = False

        if typer == BOOL_TYPE:
            if is_tvar and (not force_single_threaded):
                return 'LockedTrueFalseVariable'
            return 'SingleThreadedLockedTrueFalseVariable'
        elif typer == NUMBER_TYPE:
            if is_tvar and (not force_single_threaded):
                return 'LockedNumberVariable'
            return 'SingleThreadedLockedNumberVariable'        
        elif typer == STRING_TYPE:
            if is_tvar and (not force_single_threaded):
                return 'LockedTextVariable'
            return 'SingleThreadedLockedTextVariable'

    # FIXME: construct useful type from type object
    return '/** Fixme: must fill in emit_wrapped_type method.*/'


def construct_new_expression(type_object,initializer_node,emit_ctx):
    """Generates the java new expression that assign a newly-declared
    variable to.

    Args:
        type_object: {BasicType object}
        
        initializer_node: {None or AstNode} What to assign with new
        expression

        emit_ctx: {EmitContext}

    Returns:
        {string} --- Java-ized expression used on rhs of equals during
        declaration.
    """
    if isinstance(type_object,BasicType):
        initializer_text = None
        if initializer_node is not None:
            initializer_text = emit_statement(emit_ctx,initializer_node)

        java_type_text = emit_ralph_wrapped_type(type_object)
        if initializer_text is None:
            return 'new %s (_host_uuid,false)' % java_type_text
        return 'new %s (_host_uuid,false,%s)' % (java_type_text,initializer_text)
    elif isinstance(type_object,MapType):
        java_type_text = emit_ralph_wrapped_type(type_object)
        # currently, disallowing initializing maps

        index_basic_type = type_object.from_type_node.type.basic_type
        if index_basic_type == NUMBER_TYPE:
            java_map_index_type_text = (
                'SingleThreadedLockedContainer.IndexType.DOUBLE')            
        elif index_basic_type == TEXT_TYPE:
            java_map_index_type_text = (
                'SingleThreadedLockedContainer.IndexType.STRING')
        elif index_basic_type == BOOL_TYPE:
            java_map_index_type_text = (
                'SingleThreadedLockedContainer.IndexType.BOOLEAN')
        #### DEBUG
        else:
            raise InternalEmitException(
                'Map only accepts value types for indices')
        #### END DEBUG
        
        to_return = (
            'new %s(_host_uuid,false,%s)' %
            (java_type_text,java_map_index_type_text))
        return to_return
    #### DEBUG
    else:
        raise InternalEmitException(
            'Can only construct new expression from basic type or map type')
    #### END DEBUG    

def emit_map_type(type_object):
    # emit for maps
    if isinstance(type_object,MapType):
        key_type_node = type_object.from_type_node
        value_type_node = type_object.to_type_node

        key_internal_type_text = emit_internal_type(
            key_type_node.type)
        value_internal_type_text = emit_internal_type(
            value_type_node.type)
        dewaldoify_type_text = (
            'HashMap<%s,%s>' %
            (key_internal_type_text,value_internal_type_text))

        map_var_type = 'SingleThreadedMapVariable'
        if type_object.is_tvar:
            map_var_type = 'MultiThreadedMapVariable'

        print 'May not be dewaldo-ifying maps correctly'
        to_return = (
            '%s<%s,%s,%s>' %
            (map_var_type,key_internal_type_text,
             value_internal_type_text,dewaldoify_type_text))

        return to_return
    else:
        raise InternalEmitException(
            'Requires map type in emit_map_type')

    
def emit_internal_type(type_object):
    '''
    @param {Type or None} type_object --- None if type corresponds to
    void (eg., in method signature).

    @returns{String} --- Java-ized version of Ralph type: eg.,
    Double, Boolean, etc.
    '''
    if type_object is None:
        return 'void'
    else:
        # emit for map type
        if isinstance(type_object,MapType):
            return emit_map_type(type_object)
        
        # emit for basic types
        if isinstance(type_object,BasicType):
            typer = type_object.basic_type
        elif isinstance(type_object,MethodType):
            typer = type_object.returns_type
            if typer is not None:
                typer = typer.basic_type

        if typer == BOOL_TYPE:
            return 'Boolean'
        elif typer == NUMBER_TYPE:
            return 'Double'
        elif typer == STRING_TYPE:
            return 'String'
        elif typer is None:
            return 'void'

    # FIXME: construct useful type from type object
    return '/** Fixme: must fill in emit_type method.*/'

# indices are labels; values are operators should compile to.
NUMERICAL_ONLY_BINARY_LABELS_DICT = {
    ast_labels.ADD: '+',
    ast_labels.SUBTRACT: '-',
    ast_labels.MULTIPLY: '*',
    ast_labels.DIVIDE: '/'
    }
NUMERICAL_ONLY_COMPARISONS_DICT = {
    ast_labels.GREATER_THAN: '>',
    ast_labels.GREATER_THAN_EQUALS: '>=',
    ast_labels.LESS_THAN: '<',
    ast_labels.LESS_THAN_EQUALS: '<='
    }


def emit_statement(emit_ctx,statement_node):
    '''
    @param {EmitContext} emit_ctx --- Already loaded with previous
    scopes' variables, including method arguments.

    @param {AstNode} statement_node --- Can be any ast node that is
    classified as a statement in the parsing rules
    '''
    if statement_node.label in NUMERICAL_ONLY_BINARY_LABELS_DICT:
        lhs = emit_statement(emit_ctx,statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx,statement_node.rhs_expression_node)
        
        java_operator = NUMERICAL_ONLY_BINARY_LABELS_DICT[statement_node.label]
        return (
            '(new Double(%s.doubleValue() %s %s.doubleValue() ) )' %
            (lhs, java_operator, rhs))
    
    elif statement_node.label == ast_labels.NUMBER_LITERAL:
        return '(new Double(%f))' % statement_node.value

    elif statement_node.label == ast_labels.NOT:
        to_not_text = emit_statement(emit_ctx,statement_node.to_not_node)
        return '(new Boolean( ! %s.booleanValue()))' % to_not_text

    elif statement_node.label == ast_labels.AND:
        lhs = emit_statement(emit_ctx,statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx,statement_node.rhs_expression_node)
        return (
            '(new Boolean( %s.booleanValue() && %s.booleanValue()))' %
            (lhs,rhs))
    elif statement_node.label == ast_labels.OR:
        lhs = emit_statement(emit_ctx,statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx,statement_node.rhs_expression_node)
        return (
            '(new Boolean( %s.booleanValue() || %s.booleanValue()))' %
            (lhs,rhs))
    
    elif statement_node.label == ast_labels.EQUALS:
        lhs = emit_statement(emit_ctx, statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx, statement_node.rhs_expression_node)
        return '(new Boolean(%s.equals(%s)))' % (lhs,rhs)
    elif statement_node.label == ast_labels.NOT_EQUALS:
        lhs = emit_statement(emit_ctx, statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx, statement_node.rhs_expression_node)
        return '(new Boolean(! %s.equals(%s)))' % (lhs,rhs)    

    elif statement_node.label == ast_labels.ATOMICALLY:
        atomic_logic = ''
        for statement in statement_node.statement_list:
            atomic_logic += emit_statement(emit_ctx,statement)
            atomic_logic += ';\n'
        atomic_logic = indent_string(atomic_logic,2)
        
        return '''
{
    _active_event = _active_event.clone_atomic();
    while(true)
    {
// what to actually execute inside of atomic block
%s
        if (_active_event.begin_first_phase_commit())
        {
            try {
                ((RootEventParent)_active_event.event_parent).event_complete_queue.take();
            } catch (InterruptedException _ex) {
                // TODO Auto-generated catch block
                _ex.printStackTrace();
            }
            // FIXME: should actually check that the commit went through
            // before breaking.
            System.out.println("\\nWarn: must cycle in case atomic fails\\n");
            break;
        }
    }
    _active_event = _active_event.restore_from_atomic();
}
''' % atomic_logic
    
    elif statement_node.label == ast_labels.ASSIGNMENT:
        rhs_text = emit_statement(emit_ctx,statement_node.rhs_node)
        emit_ctx.set_lhs_of_assign(True)
        lhs_text = emit_statement(emit_ctx,statement_node.lhs_node)
        emit_ctx.set_lhs_of_assign(False)
        return '%s.set_val(_active_event,%s);\n' % (lhs_text,rhs_text);

    elif statement_node.label == ast_labels.IDENTIFIER_EXPRESSION:

        # first, check if call is a method.  if it is, then just emit
        # it directly.
        if isinstance(statement_node.type,MethodType):
            # check if global method or local method
            if emit_ctx.method_name_in_method_set(statement_node.value):
                # it is a global method: return the unaltered name
                # immediately.
                return statement_node.value
            else:
                return '/** Not yet supporting function objects.*/'


        # guaranteed that the identifier is for a non-method object type
        internal_var_name = emit_ctx.lookup_internal_var_name(
            statement_node.value)
        
        if internal_var_name is None:
            # internal_var_name is not declared in this scope: some
            # type of error.
            raise InternalEmitException(
                'No record of variable named %s' % statement_node.value)

        if ((not emit_ctx.get_lhs_of_assign()) and
            (not isinstance(statement_node.type,MapType))):
            # if not in lhs of assign, then actually get internal
            # value of variable.  (So can perform action on it.)

            # for maps, explicitly need to call get_val and set_val in
            # methods.  (This is because need to be able to call
            # clone_for_args method directly on exterrnal maps rather
            # than the internal map that they are wrapping.)
            internal_var_name += '.get_val(_active_event)'
        return internal_var_name

    elif statement_node.label == ast_labels.METHOD_CALL:
        # FIXME: Need to handle method calls on objects as well
        method_text = emit_statement(
            emit_ctx,
            # statement_node.method node is an identifier or dot node
            statement_node.method_node)

        # dot statements should not take in ExecutingEventContexts.
        # Eg., calling .size on a map should not call .get_len on
        # internal map and pass in a _ctx.  It should just pass in
        # _active_event.
        if statement_node.method_node.label != ast_labels.DOT:
            method_text += '(_ctx,_active_event'
        else:
            method_text += '(_active_event'
            
        for arg_node in statement_node.args_list:
            arg_text = emit_statement(emit_ctx,arg_node)
            if isinstance(arg_node.type,MapType):
                arg_text += '.clone_for_args(_active_event)'
            
            method_text += ',' + arg_text
        method_text += ')'
        return method_text


    elif statement_node.label == ast_labels.PARTNER_METHOD_CALL:
        rpc_args = []

        #FIXME: only allowing putting identifiers into arguments of
        #remote method call because must create RPCArgObjects.
        prev_lhs_of_assign = emit_ctx.get_lhs_of_assign()
        emit_ctx.set_lhs_of_assign(True)
        for rpc_arg_node in statement_node.args_list:
            # FIXME: Setting false here, which disallows sending arg
            # as reference.
            rpc_arg_text = (
                'new RPCArgObject( %s,false )' %
                emit_statement(emit_ctx,rpc_arg_node))
            rpc_args.append(rpc_arg_text)
        emit_ctx.set_lhs_of_assign(prev_lhs_of_assign)
            
        rpc_args_list_text = (
            'new ArrayList<RPCArgObject>( Arrays.asList(%s))' %
            ','.join(rpc_args))
        
        partner_call_text = '''
_ctx.hide_partner_call(
    this, _active_event,"%s",true, //whether or not first method call
    %s)''' %  ( statement_node.partner_method_name, rpc_args_list_text)

        return partner_call_text
    
    elif statement_node.label in NUMERICAL_ONLY_COMPARISONS_DICT:
        lhs = emit_statement(emit_ctx, statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx, statement_node.rhs_expression_node)
        comparison = NUMERICAL_ONLY_COMPARISONS_DICT[statement_node.label]
        return (
            '(new Boolean(%s.doubleValue() %s %s.doubleValue()))' %
            (lhs,comparison,rhs))

    elif statement_node.label == ast_labels.TRUE_FALSE_LITERAL:
        internal = 'false'
        if statement_node.value:
            internal = 'true'
        return '(new Boolean(%s))' % internal

    elif statement_node.label == ast_labels.TEXT_LITERAL:
        return '("' + statement_node.value + '")'
    
    elif statement_node.label == ast_labels.DECLARATION_STATEMENT:
        java_type_statement = emit_ralph_wrapped_type(statement_node.type)
        new_expression = construct_new_expression(
            statement_node.type,statement_node.initializer_node,emit_ctx)

        # add new variable to emit_ctx stack
        emit_ctx.add_var_name(statement_node.var_name)
        internal_var_name = emit_ctx.lookup_internal_var_name(
            statement_node.var_name)

        declaration_statement = (
            '%s %s = %s;' %
            (java_type_statement,internal_var_name,new_expression))

        # should not add to var context if we're in the middle of
        # declaring endpoint global variables.  
        if not emit_ctx.get_in_endpoint_global_vars():
            # no ; at end, because caller will place one on.
            context_stack_push_statement = (
                '_ctx.var_stack.add_var("%s",%s)' % 
                (internal_var_name,internal_var_name))
        else:
            declaration_statement = 'private ' + declaration_statement;
            context_stack_push_statement = ''
            
        return (
            declaration_statement + '\n' + context_stack_push_statement)

    elif statement_node.label == ast_labels.RETURN:
        return_text = '_ctx.var_stack.pop();\n'
        return_text += 'return'
        if statement_node.what_to_return_node is not None:
            return_text += ' ' + emit_statement(
                emit_ctx,statement_node.what_to_return_node)
        return return_text
    
    elif statement_node.label == ast_labels.CONDITION:

        if_text = emit_statement(emit_ctx,statement_node.if_node)
        elif_text = '\n'
        for elif_node in statement_node.elifs_list:
            elif_text += emit_statement(emit_ctx,elif_node)
            elif_text += '\n'
        else_text = ''
        if statement_node.else_node_body is not None:
            else_text = 'else {\n'
            else_text_body = emit_statement(emit_ctx,statement_node.else_node_body)
            else_text += indent_string(else_text_body)
            else_text += '\n}\n'

        return if_text + elif_text + else_text

    elif statement_node.label == ast_labels.IF:
        predicate_text = emit_statement(emit_ctx,statement_node.predicate_node)
        if_text = 'if (%s.booleanValue()){\n' % predicate_text
        if_body_text = emit_statement(emit_ctx,statement_node.body_node)
        if_text += indent_string(if_body_text) + '\n}\n'
        return if_text

    elif statement_node.label == ast_labels.ELIF:
        predicate_text = emit_statement(emit_ctx,statement_node.predicate_node)
        elif_text = 'else if (%s.booleanValue()){\n' % predicate_text
        elif_body_text = emit_statement(emit_ctx,statement_node.body_node)
        elif_text += indent_string(elif_body_text) + '\n}\n'
        return elif_text
    
    elif statement_node.label == ast_labels.SCOPE:
        to_return = '{\n'
        
        scope_body_text = '_ctx.var_stack.push(false);\n'

        # Any variable declared in this scope should be removed after
        # this scope statement: so push on a scope to emit_ctx and
        # after emitting individual statements (ie, at end of for
        # loop, pop off of emit_ctx).
        emit_ctx.push_scope()
        for individual_statement_node in statement_node.statement_list:
            scope_body_text += emit_statement(emit_ctx,individual_statement_node)
            scope_body_text += ';\n'
        emit_ctx.pop_scope()

        scope_body_text +='\n_ctx.var_stack.pop();'
        to_return += indent_string(scope_body_text) + '\n}\n'
        return to_return

    elif statement_node.label == ast_labels.DOT:
        return emit_dot_statement(emit_ctx,statement_node)
    return (
        '\n/** FIXME: must fill in emit_method_body for label %s */\n' %
        statement_node.label)


def emit_dot_statement(emit_ctx,dot_node):
    """
    """
    left_of_dot_node = dot_node.left_of_dot_node
    right_of_dot_node = dot_node.right_of_dot_node
    to_return = emit_statement(emit_ctx,left_of_dot_node)

    if isinstance(left_of_dot_node.type,MapType):
        if right_of_dot_node.label != ast_labels.IDENTIFIER_EXPRESSION:
            raise InternalEmitException(
                'Expected identifier to the right of %s' %
                statement_node.value)
        right_hand_side_method = right_of_dot_node.value
        if right_hand_side_method == MapType.SIZE_METHOD_NAME:
            to_return += '.get_val(_active_event).get_len_boxed'
        elif right_hand_side_method == MapType.CONTAINS_METHOD_NAME:
            to_return += '.get_val(_active_event).contains_key_called_boxed'
        elif right_hand_side_method == MapType.GET_METHOD_NAME:
            to_return += '.get_val(_active_event).get_val_on_key'
        elif right_hand_side_method == MapType.SET_METHOD_NAME:
            to_return += '.get_val(_active_event).set_val_on_key'
        #### DEBUG
        else:
            raise InternalEmitException(
                'Unknown identifier on rhs of dot for map.')
        #### END DEBUG
    else:
        raise InternalEmitException(
            'Unknown dot statement: not dot on map.')
        
    return to_return
