from ralph.parse.parse_util import InternalParseException,ParseException
from ralph.parse.parse_util import TypeCheckException
import ralph.parse.ast_labels as ast_labels
from ralph.parse.type import BasicType, MethodType
from ralph.parse.type_check_context import TypeCheckContext

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

    def _append_child(self,child_node):
        '''
        Should be protected method, but do not have decorator for it
        '''
        self.children.append(child_node)
    def _prepend_child(self,child_node):
        self.children.insert(0,child_node)

    def type_check(self,type_check_ctx):
        """Type check this statement.
        """
        print '\nPure virtual type check in AstNode.\n'
        assert(False)
        
class RootStatementNode(_AstNode):
    def __init__(self,endpoint_node_list):
        super(RootStatementNode,self).__init__(
            ast_labels.ROOT_STATEMENT,0,None)
        
        #### DEBUG
        if endpoint_node_list.label != ast_labels.ENDPOINT_LIST_STATEMENT:
            raise InternalParseException(
                'RootStatementNode requires endpoint list statement node')
        #### END DEBUG

        self.endpoint_node_list = list(endpoint_node_list)

    def type_check(self):
        for endpt_node in self.endpoint_node_list:
            endpt_node.type_check()
        
        
class EndpointDefinitionNode(_AstNode):
    def __init__(self,name_identifier_node,endpoint_body_node,line_number):
        super(EndpointDefinitionNode,self).__init__(
            ast_labels.ENDPOINT_DEFINITION_STATEMENT,line_number)

        self.name = name_identifier_node.get_value()
        self.body_node = endpoint_body_node

    def type_check(self):
        type_check_ctx = TypeCheckContext(self.name)
        type_check_ctx.push_scope()
        self.body_node.type_check(type_check_ctx)


class EndpointBodyNode(_AstNode):
    def __init__(self):
        super(EndpointBodyNode,self).__init__(
            ast_labels.ENDPOINT_BODY,0)
        self.variable_declaration_nodes = []
        self.method_declaration_nodes = []
        
    def prepend_variable_declaration_node(
        self,variable_declaration_node):
        self.variable_declaration_nodes.insert(0,variable_declaration_node)

    def prepend_method_declaration_node(
        self,method_declaration_node):
        self.method_declaration_nodes.insert(0,method_declaration_node)
        
    def type_check(self,type_check_ctx):
        # First populate global scope with all endpoint nodes.
        for variable_declaration_node in self.variable_declaration_nodes:
            variable_declaration_node.type_check(type_check_ctx)
            
        # Populate every method signature in ctx
        for method_declaration_node in self.method_declaration_nodes:
            method_name = method_declaration_node.method_name
            method_type = method_declaration_node.method_signature_node.type
            type_check_ctx.add_var_name(method_name,method_type)

        # Type check the body of each method
        for method_declaration_node in self.method_declaration_nodes:
            method_declaration_node.type_check(type_check_ctx)
        
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

class MethodDeclarationNode(_AstNode):
    def __init__(
        self,method_signature_node,scope_body_node ):

        super(MethodDeclarationNode,self).__init__(
            ast_labels.METHOD_DECLARATION,
            method_signature_node.line_number,
            method_signature_node.type)

        self.method_name = method_signature_node.get_method_name()
        self.method_signature_node = method_signature_node
        self.method_body_statement_list = scope_body_node.get_statement_list()

    def type_check(self,type_check_ctx):
        """

        Note: does not insert type name and signature into
        type_check_ctx.  Should have already been inserted in
        EndpointBodyNode.
        """
        print '\nFinish type checking method body\n'

        
class MethodSignatureNode(_AstNode):
    def __init__(
        self,method_name_identifier_node,method_declaration_args_node,
        returns_type_node):
        '''
        @params{}
        @params{}
        
        @params{VariableTypeNode or None} --- None if doesn't return
        anything
        '''
        super(MethodSignatureNode,self).__init__(
            ast_labels.METHOD_SIGNATURE,
            method_name_identifier_node.line_number)

        self.method_name = method_name_identifier_node.get_value()
        self.method_declaration_args = method_declaration_args_node.to_list()

        # FIXME: type should also include arguments                                        
        self.type = None
        if returns_type_node is not None:
            self.type = returns_type_node.type

    def get_method_name(self):
        return self.method_name


class MethodDeclarationArgNode(_AstNode):
    def __init__(self, variable_type_node, name_identifier_node):
        super(MethodDeclarationArgNode,self).__init__(
            ast_labels.METHOD_DECLARATION_ARG,
            variable_type_node.line_number,variable_type_node.type)

        self.arg_name = name_identifier_node.get_value()

class AtomicallyNode(_AstNode):
    def __init__(self,scope_node):

        super(AtomicallyNode,self).__init__(
            ast_labels.ATOMICALLY,scope_node.line_number)

        self.statement_list = scope_node.get_statement_list()
        
class ScopeNode (_AstNode):
    def __init__(self,scope_body_node):
        super(ScopeNode,self).__init__(
            ast_labels.SCOPE,scope_body_node.line_number)
        
        self.statement_list = scope_body_node.get_statement_list()
    def get_statement_list(self):
        return list(self.statement_list)
        
class ParallelNode(_AstNode):
    def __init__(
        self,to_iter_over_expression_node,lambda_expression_node,line_number):
        super(ParallelNode,self).__init__(ast_labels.PARALLEL,line_number)
        self.to_iter_over_expression_node = to_iter_over_expression_node
        self.lambda_expression_node = lambda_expression_node
    
class AssignmentNode(_AstNode):
    def __init__(self,lhs_node,rhs_node):
        super(AssignmentNode,self).__init__(
            ast_labels.ASSIGNMENT,lhs_node.line_number)
        self.lhs_node = lhs_node
        self.rhs_node = rhs_node

class NotNode(_AstNode):
    def __init__(self,to_not_node):
        super(NotNode,self).__init__(
            ast_labels.NOT,to_not_node.line_number)
        
        self.to_not_node = to_not_node

class LenNode(_AstNode):
    def __init__(self,len_of_node,line_number):
        super(LenNode,self).__init__(
            ast_labels.LEN,line_number)
        
        self.len_of_node = len_of_node

class ReturnNode(_AstNode):
    def __init__(self,line_number):
        super(ReturnNode,self).__init__(ast_labels.RETURN,line_number)
        self.what_to_return_node = None
    def add_return_expression_node(self,what_to_return_node):
        self.what_to_return_node = what_to_return_node
        
class ConditionNode(_AstNode):
    def __init__(self,if_node,elifs_node,else_node):
        '''
        @param {ElseNode} else_node --- Can have a body of None.
        '''
        super(ConditionNode,self).__init__(
            ast_labels.CONDITION,if_node.line_number)

        self.if_node = if_node
        self.elifs_list = elifs_node.get_else_if_node_list()
        # else_none_body may be None
        self.else_node_body = else_node.body_node

        
        
class BracketNode(_AstNode):
    def __init__(self,outside_bracket_node,inside_bracket_node):
        '''
        @param {_AstNode} outside_bracket_node --- variable_node is
        either an identifier node, bracket node, or dot node
        '''
        super(BracketNode,self).__init__(
            ast_labels.BRACKET,outside_bracket_node.line_number)
        
        self.outside_bracket_node = outside_bracket_node
        self.inside_bracket_node = inside_bracket_node

class DotNode(_AstNode):
    def __init__(self,left_of_dot_node, right_of_dot_node):
        super(DotNode,self).__init__(
            ast_labels.DOT,left_of_dot_node.line_number)
        
        self.left_of_dot_node = left_of_dot_node
        self.right_of_dot_node = right_of_dot_node


class MethodCallNode(_AstNode):
    def __init__(self,variable_node,method_call_args_node):
        super(MethodCallNode,self).__init__(
            ast_labels.METHOD_CALL,variable_node.line_number)
        
        self.method_node = variable_node
        self.args_list = method_call_args_node.get_args_list()

class RangeExpressionNode(_AstNode):
    def __init__(
        self,start_expression_node,increment_expression_node,
        end_expression_node,line_number):
        
        super(RangeExpressionNode,self).__init__(
            ast_labels.RANGE,line_number)

        # where range statement starts from
        self.start_expression_node = start_expression_node
        # how much to increment for each value
        self.increment_expression_node = increment_expression_node
        # where to end range expression
        self.end_expression_node = end_expression_node
        
        
class _LiteralNode(_AstNode):
    '''
    Parent class of NumberLiteralNode, TextLiteralNode,
    TrueFalseLiteralNode
    '''
    def __init__(self,label,value,line_number):
        super(_LiteralNode,self).__init__(label,line_number)
        self.line_number = line_number
        self.value = value
        
class NumberLiteralNode(_LiteralNode):
    def __init__(self,number,line_number):
        super(NumberLiteralNode,self).__init__(
            ast_labels.NUMBER_LITERAL,number,line_number)
        
class TextLiteralNode(_LiteralNode):
    def __init__(self,text,line_number):
        super(TextLiteralNode,self).__init__(
            ast_labels.TEXT_LITERAL,text,line_number)
        
class TrueFalseLiteralNode(_LiteralNode):
    def __init__(self,true_false,line_number):
        super(TrueFalseLiteralNode,self).__init__(
            ast_labels.TRUE_FALSE_LITERAL,true_false,line_number)
        
class VariableTypeNode(_AstNode):
    def __init__(self,basic_type,is_tvar,line_number):
        
        super(VariableTypeNode,self).__init__(
            ast_labels.VARIABLE_TYPE,line_number)

        self.type = self._build_type(basic_type,is_tvar)
        
    def _build_type(self,basic_type,is_tvar):
        return BasicType(basic_type,is_tvar)

    
class _BinaryExpressionNode(_AstNode):
    def __init__(
        self,label,lhs_expression_node,rhs_expression_node):

        super (_BinaryExpressionNode,self).__init__(
            label,lhs_expression_node.line_number)
        self.lhs_expression_node = lhs_expression_node
        self.rhs_expression_node = rhs_expression_node

class MultiplyExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(MultiplyExpressionNode,self).__init__(
            ast_labels.MULTIPLY,lhs_expression_node,rhs_expression_node)

        
class DivideExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(DivideExpressionNode,self).__init__(
            ast_labels.DIVIDE,lhs_expression_node,rhs_expression_node)

class AddExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(AddExpressionNode,self).__init__(
            ast_labels.ADD,lhs_expression_node,rhs_expression_node)

class SubtractExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(SubtractExpressionNode,self).__init__(
            ast_labels.SUBTRACT,lhs_expression_node,rhs_expression_node)
class GreaterThanExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(GreaterThanExpressionNode,self).__init__(
            ast_labels.GREATER_THAN,lhs_expression_node,rhs_expression_node)
class GreaterThanEqualsExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(GreaterThanEqualsExpressionNode,self).__init__(
            ast_labels.GREATER_THAN_EQUALS,lhs_expression_node,rhs_expression_node)
class LessThanExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(LessThanExpressionNode,self).__init__(
            ast_labels.LESS_THAN,lhs_expression_node,rhs_expression_node)
class LessThanEqualsExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(LessThanEqualsExpressionNode,self).__init__(
            ast_labels.LESS_THAN_EQUALS,lhs_expression_node,rhs_expression_node)
class EqualsExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(EqualsExpressionNode,self).__init__(
            ast_labels.EQUALS,lhs_expression_node,rhs_expression_node)
class NotEqualsExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(NotEqualsExpressionNode,self).__init__(
            ast_labels.NOT_EQUALS,lhs_expression_node,rhs_expression_node)
class AndExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(AndExpressionNode,self).__init__(
            ast_labels.AND,lhs_expression_node,rhs_expression_node)
class OrExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(OrExpressionNode,self).__init__(
            ast_labels.OR,lhs_expression_node,rhs_expression_node)
class InExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(InExpressionNode,self).__init__(
            ast_labels.IN,lhs_expression_node,rhs_expression_node)
class NotInExpressionNode(_BinaryExpressionNode):
    def __init__(self,lhs_expression_node,rhs_expression_node):
        super(NotInExpressionNode,self).__init__(
            ast_labels.NOT_IN,lhs_expression_node,rhs_expression_node)

        
        
def create_binary_expression_node(
    operator,lhs_expression_node,rhs_expression_node):

    if operator == '*':
        return MultiplyExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == '/':
        return DivideExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == '+':
        return AddExpressionNode(lhs_expression_node,rhs_expression_node)    
    elif operator == '-':
        return SubtractExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == '>':
        return GreaterThanExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == '>=':
        return GreaterThanEqualsExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == '<':
        return LessThanExpressionNode(lhs_expression_node,rhs_expression_node)    
    elif operator == '<=':
        return LessThanEqualsExpressionNode(lhs_expression_node,rhs_expression_node)    
    elif operator == '==':
        return EqualsExpressionNode(lhs_expression_node,rhs_expression_node)    
    elif operator == '!=':
        return NotEqualsExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == 'and':
        return AndExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == 'or':
        return OrExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == 'in':
        return InExpressionNode(lhs_expression_node,rhs_expression_node)
    elif operator == 'not in':
        return NotInExpressionNode(lhs_expression_node,rhs_expression_node)
    
    raise InternalParseException(
        'Unknown binary operator when creating binary expression')

    
    
#### Intermediate nodes that get removed from actual AST ####
    
class EndpointListNode(_AstNode):
    def __init__(self,endpoint_definition_node):
        super(EndpointListNode,self).__init__(
            ast_labels.ENDPOINT_LIST_STATEMENT,0)

        self._append_child(endpoint_definition_node)

    def prepend_endpoint_definition(self,endpoint_definition_node):
        self._prepend_child(endpoint_definition_node)
        
    def __iter__(self):
        return iter(self.children)

class EmptyNode(_AstNode):
    def __init__(self):
        super(EmptyNode,self).__init__(
            ast_labels.EMPTY_STATEMENT,0)

class MethodDeclarationArgsNode(_AstNode):
    def __init__(self):
        super(MethodDeclarationArgsNode,self).__init__(
            ast_labels.METHOD_DECLARATION_ARGS,0)
    def prepend_method_declaration_arg(self,method_declaration_arg):
        self._prepend_child(method_declaration_arg)

    def to_list(self):
        return list(self.children)
        
class ScopeBodyNode(_AstNode):
    def __init__(self,line_number):
        super(ScopeBodyNode,self).__init__(
            ast_labels.SCOPE_BODY,line_number)
        
    def prepend_statement_node(self,statement_node):
        self._prepend_child(statement_node)

    def get_statement_list(self):
        '''
        @returns{list} --- Each element is an ast node ordered by the
        order that it appears in the method's body.
        '''
        return list(self.children)

class MethodCallArgsNode(_AstNode):
    def __init__(self,line_number):
        super(MethodCallArgsNode,self).__init__(
            ast_labels.METHOD_CALL_ARGS,line_number)

    def prepend_arg(self,expression_node):
        self._prepend_child(expression_node)

    def get_args_list(self):
        return list(self.children)


class IfNode(_AstNode):
    def __init__(self,predicate_node,if_body_node,line_number):
        super(IfNode,self).__init__(ast_labels.IF,line_number)

        self.predicate_node = predicate_node
        self.body_node = if_body_node

class ElseIfNodes(_AstNode):
    def __init__(self):
        super(ElseIfNodes,self).__init__(ast_labels.ELSE_IFS,0)
        # each element of children is an if ndoe

    def prepend_else_if(self,else_if_node):
        '''
        @param {IfNode} else_if_node --- Because of similar structure,
        each else_if_node is an IfNode.
        '''
        self._prepend_child(else_if_node)
        
    def get_else_if_node_list(self):
        return list(self.children)

class ElseNode (_AstNode):
    def __init__(self,line_number):
        super(ElseNode,self).__init__(ast_labels.ELSE,line_number)
        self.body_node = None
    def add_else_body_node (self,body_node):
        self.body_node = body_node
        
    
