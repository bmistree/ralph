import ralph.parse.ast_labels as ast_labels
from ralph.java_emit.emit_utils import indent_string
from ralph.java_emit.emit_context import EmitContext
from ralph.parse.type import BasicType,MethodType
from ralph.parse.ast_labels import BOOL_TYPE, NUMBER_TYPE, STRING_TYPE

def emit(root_node,package_name,program_name):
    '''
    @param {RootStatementNode} root_node 
    
    @returns {String} --- Emitted program text
    '''

    prog_txt = '''
package %s;

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
    endpt_class_signature = 'public static class %s { \n' % endpt_node.name
    
    endpt_class_body = emit_endpt_variable_declarations(
        endpt_node.body_node.variable_declaration_nodes)
    endpt_class_body += '\n'
    endpt_class_body += emit_endpt_method_declarations(
        endpt_node.body_node.method_declaration_nodes)
    endpt_class_body += '\n'
    return endpt_class_signature + indent_string(endpt_class_body) + '\n}'

def emit_endpt_variable_declarations(variable_declaration_node_list):
    '''
    @param {list} variable_declaration_node_list --- Each element is
    a DeclarationStatementNode.
    '''
    return "/**\nWarn: Skipping endpoint's variable declarations.\n*/"

def emit_endpt_method_declarations(method_declaration_node_list):
    '''
    @param {list} method_declaration_node_list --- Each element is a
    MethodDeclarationNode.
    '''
    to_return = ''

    for method_declaration_node in method_declaration_node_list:
        to_return += emit_method_declaration_node(method_declaration_node)
        to_return += '\n'

    return to_return

def emit_method_declaration_node(method_declaration_node):
    '''
    @param {MethodDeclarationNode} method_declaration_node
    '''
    emit_ctx = EmitContext()
    emit_ctx.push_scope()
    signature_plus_head = emit_method_signature_plus_head(
        emit_ctx,
        method_declaration_node.method_signature_node)

    body = ''
    for statement in method_declaration_node.method_body_statement_list:
        body += emit_statement(emit_ctx,statement)
        body += '\n'
    emit_ctx.pop_scope()
    
    body += '_ctx.var_stack.pop();'
    return signature_plus_head + indent_string(body) + '\n}'


def emit_method_signature_plus_head(emit_ctx,method_signature_node):
    '''
    @param {EmitContext} emit_ctx --- Loads arguments to method into
    emit_ctx.

    @param {MethodSignatureNode} method_signature_node ---

    @returns {String} --- A java signature for method.  Eg.,

    public Double some_method (
        ExecutingEventContext _ctx, LockedActiveEvent _active_event,
        SomeType SomeVar)
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
        'public %s %s (' % (return_type, method_signature_node.method_name) )

    argument_text_list = []
    for argument_node in method_signature_node.method_declaration_args:
        argument_type_text = emit_internal_type(argument_node.type)
        argument_name_text = argument_node.arg_name
        argument_text_list.append(
            argument_type_text + ' ' + argument_name_text)

    to_return += ','.join(argument_text_list) + ')'

    # 3: emit head section where add to scope stack and push arguments
    # on to scope stack.  Must push arguments on to scope stack so
    # they're available in defer statements
    to_return += '\n{\n'
    to_return += indent_string(
        '\n_ctx.var_stack.push(true);//true because func scope\n');
    for argument_text in argument_text_list:
        to_return += indent_string(
            '\n_ctx.var_stack.add_var("%s",%s);\n' %
            (argument_text,argument_text))

    return to_return


def emit_ralph_wrapped_type(type_object):
    '''
    @param {Type or None} type_object --- None if type corresponds to
    void (eg., in method signature).

    @returns{String} --- Java-ized version of wrapped Ralph type: eg.,
    LockedNumberVarialbe, etc.
    '''
    if type_object is None:
        return 'void'
    else:
        if isinstance(type_object,BasicType):
            typer = type_object.basic_type
            is_tvar = type_object.is_tvar
        elif isinstance(type_object,MethodType):
            typer = type_object.returns_type
            is_tvar = False
            
        if typer == BOOL_TYPE:
            if is_tvar:
                return 'LockedTrueFalseVariable'
            return 'SingleThreadedLockedTrueFalseVariable'
        elif typer == NUMBER_TYPE:
            if is_tvar:
                return 'LockedNumberVariable'
            return 'SingleThreadedLockedNumberVariable'        
        elif typer == STRING_TYPE:
            if is_tvar:
                return 'LockedTextVariable'
            return 'SingleThreadedLockedTextVariable'

    # FIXME: construct useful type from type object
    return '/** Fixme: must fill in emit_type method.*/'


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
        if isinstance(type_object,BasicType):
            typer = type_object.basic_type
        elif isinstance(type_object,MethodType):
            typer = type_object.returns_type

        if typer == BOOL_TYPE:
            return 'Boolean'
        elif typer == NUMBER_TYPE:
            return 'Double'
        elif typer == STRING_TYPE:
            return 'String'        

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
            '(new Double(%s.doubleValue() %s %s.doublevalue() ) )' %
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
        return '(new Boolean((%s.equals(%s)))' % (lhs,rhs)
    elif statement_node.label == ast_labels.NOT_EQUALS:
        lhs = emit_statement(emit_ctx, statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx, statement_node.rhs_expression_node)
        return '(new Boolean(! %s.equals(%s)))' % (lhs,rhs)    

    elif statement_node.label == ast_labels.ASSIGNMENT:
        rhs_text = emit_statement(emit_ctx,statement_node.rhs_node)
        emit_ctx.set_lhs_of_assign(True)
        lhs_text = emit_statement(emit_ctx,statement_node.lhs_node)
        emit_ctx.set_lhs_of_assign(False)
        return '%s.set_val(_active_event,%s);\n' % (lhs_text,rhs_text);

    elif statement_node.label == ast_labels.IDENTIFIER_EXPRESSION:
        internal_var_name = emit_ctx.lookup_internal_var_name(
            statement_node.value)
        if internal_var_name is None:
            # internal_var_name is not declared in this scope: it's an
            # endpoint global variable.
            type_text = emit_ralph_wrapped_type(statement_node.type)
            internal_var_name = '''
((%s)_active_event.event_parent.global_var_stack.get_var_if_exists("%s"))
''' % (type_text,statement_node.value)

        if not emit_ctx.get_lhs_of_assign():
            # if not in lhs of assign, then actually get internal
            # value of variable.  (So can perform action on it.)
            internal_var_name += '.get_val(_active_event)'
        return internal_var_name
    
    elif statement_node.label in NUMERICAL_ONLY_COMPARISONS_DICT:
        lhs = emit_statement(emit_ctx, statement_node.lhs_expression_node)
        rhs = emit_statement(emit_ctx, statement_node.rhs_expression_node)
        comparison = NUMERICAL_ONLY_COMPARISONS_DICT[statement_node.label]
        return (
            '(new Boolean(%s.doubleValue() %s %s.doubleValue()))' %
            (lhs,comparison,rhs))

    elif statement_node.label == ast_labels.SCOPE:
        to_return = '''
_ctx.var_stack.push(false);
'''
        # Any variable declared in this scope should be removed after
        # this scope statement: so push on a scope to emit_ctx and
        # after emitting individual statements (ie, at end of for
        # loop, pop off of emit_ctx).
        emit_ctx.push_scope()
        for individual_statement_node in statement_node.statement_list:
            to_return += emit_statement(emit_ctx,individual_statement_node)
        emit_ctx.pop_scope()

        to_return += '''
_ctx.var_stack.pop();
'''
    
    return '\n/** FIXME: must fill in emit_method_body*/\n'

