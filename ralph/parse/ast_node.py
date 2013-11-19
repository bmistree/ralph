from ralph.parse.parse_util import InternalParseException,ParseException
import ralph.parse.ast_labels as ast_labels
from ralph.parse.type import Type

class _AstNode(object):

    def __init__(self,label,line_number,node_type=None):
        '''
        @param {ast_labels} label --- Eg., whether this node
        corresponds to a for statement, a condition statement, a
        number, etc.

        @param {types or None} node_type --- Only expressions have
        types.  And they only get types after type checking has
        completed.
        '''
        self.label = label
        self.line_number = line_number
        self.type = node_type
        self.children = []

        
    def _add_child(self,child_node):
        '''
        Should be protected method, but do not have decorator for it
        '''
        self.children.append(child_node)
    def _prepend_child(self,child_node):
        self.children.insert(0,child_node)
        
        
class RootStatementNode(_AstNode):
    def __init__(self,endpoint_node_list):
        super(RootStatementNode,self).__init__(ast_labels.ROOT_STATEMENT,0,None)
        
        #### DEBUG
        if endpoint_node_list.label != ast_labels.ENDPOINT_LIST_STATEMENT:
            raise InternalParseException(
                'RootStatementNode requires endpoint list statement node')
        #### END DEBUG
        
        for endpoint_node in endpoint_node_list:
            self._add_child(endpoint_node)

class EndpointDefinitionNode(_AstNode):
    def __init__(self,name_identifier_node,endpoint_body_node,line_number):
        super(EndpointDefinitionNode,self).__init__(
            ast_labels.ENDPOINT_DEFINITION_STATEMENT,line_number)

        self.name = name_identifier_node.get_value()
        self.body_node = endpoint_body_node


class EndpointBodyNode(_AstNode):
    def __init__(self):
        super(EndpointBodyNode,self).__init__(
            ast_labels.ENDPOINT_BODY,0)
        self.variable_declaration_nodes = []
        self.function_declaration_nodes = []
        
    def prepend_variable_declaration_node(
        self,variable_declaration_node):
        self.variable_declaration_nodes.insert(0,variable_declaration_node)

    def prepend_function_declaration_node(
        self,function_declaration_node):
        self.function_declaration_nodes.insert(0,function_declaration_node)
        
        
class IdentifierNode(_AstNode):
    def __init__(self,value,line_number):
        super(IdentifierNode,self).__init__(
            ast_labels.IDENTIFIER_EXPRESSION,line_number)

        self.value = value

    def get_value(self):
        return self.value

class DeclarationStatementNode(_AstNode):
    def __init__(
        self,var_name_identifier_node,type_node,
        initializer_node = None):
        
        super(DeclarationStatementNode,self).__init__(
            ast_labels.DECLARATION_STATEMENT,
            type_node.line_number,type_node.type)


        self.var_name = var_name_identifier_node.get_value()
        self.initializer_node = initializer_node
        

class VariableTypeNode(_AstNode):
    def __init__(self,basic_type,is_tvar,line_number):
        
        super(VariableTypeNode,self).__init__(
            ast_labels.VARIABLE_TYPE,line_number)

        self.type = self._build_type(basic_type,is_tvar)
        
    def _build_type(self,basic_type,is_tvar):
        return Type(basic_type,is_tvar)
    
        
#### Intermediate nodes that get removed from actual AST ####
class EndpointListNode(_AstNode):
    def __init__(self,endpoint_definition_node):
        super(EndpointListNode,self).__init__(
            ast_labels.ENDPOINT_LIST_STATEMENT,0)

        self._add_child(endpoint_definition_node)

    def prepend_endpoint_definition(self,endpoint_definition_node):
        self._prepend_child(endpoint_definition_node)
        
    def __iter__(self):
        return iter(self.children)

class EmptyNode(_AstNode):
    def __init__(self):
        super(EmptyNode,self).__init__(
            ast_labels.EMPTY_STATEMENT,0)
