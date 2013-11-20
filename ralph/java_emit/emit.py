
from ralph.java_emit.emit_utils import indent_string
from ralph.java_emit.emit_context import EmitContext

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
    signature = emit_method_signature(
        emit_ctx,
        method_declaration_node.method_signature_node)

    body = emit_method_body(
        emit_ctx,
        method_declaration_node.method_body_node)
    return signature + '{\n' + indent_string(body) + '\n}'

def emit_method_signature(emit_ctx,method_signature_node):
    '''
    @param {EmitContext} emit_ctx --- Loads arguments to method into
    emit_ctx.

    @param {MethodSignatureNode} method_signature_node ---

    @returns {String} --- A java signature for method.  Eg.,

    public Double some_method ()

    (Note: No '{')
    '''

    # 1: update context with loaded arguments

    # FIXME: actually load emit_ctx with args
    print '\nFIXME: must load emit_ctx with args\n'
    
    # 2: construct signature to return
    return_type = 'void'
    if method_signature_node.type is not None:
        return_type = emit_type(method_signature_node.type)

    to_return = (
        'public %s %s (' % (return_type, method_signature_node.method_name))

    
    argument_text_list = []
    for argument_node in method_signature_node.method_declaration_args:
        argument_type_text = emit_type(argument_node.type)
        argument_name_text = argument_node.arg_name
        argument_text_list.append(
            argument_type_text + ' ' + argument_name_text)

    to_return += ','.join(argument_text_list) + ')'
    return to_return


def emit_type(type_object):
    '''
    @param {Type or None} type_object --- None if type corresponds to
    void (eg., in method signature).

    @returns{String} --- Java-ized version of Ralph type: eg.,
    LockedNumberVariable.
    '''

    if type_object is None:
        return 'void'

    # FIXME: construct useful type from type object
    return '/** Fixme: must fill in emit_type method.*/'

    
def emit_method_body(emit_ctx,scope_body_node):
    '''
    @param {EmitContext} emit_ctx --- Already loaded with previous
    scopes' variables, including method arguments.

    @param {ScopeBodyNode} scope_body_node --- 
    '''
    # FIXME: must implement method
    return '\n/** FIXME: must fill in emit_method_body*/\n'

