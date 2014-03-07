from ralph.lex.ralph_lex import tokens,construct_lexer
from ralph.lex.ralph_lex import STRUCT_TYPE_TOKEN,PRINT_TYPE_TOKEN
from ralph.lex.ralph_lex import ENDPOINT_TOKEN, VERBATIM_TOKEN
from ralph.lex.ralph_lex import SERVICE_TOKEN, INTERFACE_TOKEN
from ralph.lex.ralph_lex import SERVICE_FACTORY_TOKEN,SPECULATE_TOKEN
from ralph.lex.ralph_lex import SPECULATE_CONTAINER_INTERNALS_TOKEN
from ralph.lex.ralph_lex import SPECULATE_ALL_TOKEN
import deps.ply.yacc as yacc
from ralph.parse.ast_node import *
from ralph.parse.parse_util import InternalParseException,ParseException
import ralph.parse.ast_labels

#note: global variable used by yacc.  Without this set, otherwise,
#starts at first rule.
start = 'RootStatement'
#need to have something named lexer for parser to chew into
lexer = None
# keep track for error messages.  set when initially construct the
# parser
global_parsing_filename = 'unknown'


def p_RootStatement(p):
    '''
    RootStatement : AliasList StructList EndpointList
    '''
    alias_list_node = p[1]
    struct_list_node = p[2]
    endpoint_list_node = p[3]
    p[0] = RootStatementNode(
        global_parsing_filename,alias_list_node,struct_list_node,
        endpoint_list_node)
    
def p_AliasList(p):
    '''
    AliasList : AliasList AliasStatement
              | Empty
    '''
    if len(p) == 2:
        alias_list_node = AliasListNode(global_parsing_filename)
    else:
        alias_list_node = p[1]
        alias_statement_node = p[2]
        alias_list_node.add_alias_node(alias_statement_node)
        
    p[0] = alias_list_node

def p_AliasStatement(p):
    '''
    AliasStatement : ALIAS STRUCT_TYPE Identifier AS String SEMI_COLON
                   | ALIAS ENDPOINT Identifier AS String SEMI_COLON
                   | ALIAS SERVICE Identifier AS String SEMI_COLON
                   | ALIAS INTERFACE Identifier AS String SEMI_COLON
    '''
    line_number = p.lineno(1)
    identifier_node = p[3]
    to_alias_to_string_node = p[5]

    for_struct = False
    endpoint_or_struct_token = p[2]
    if endpoint_or_struct_token == STRUCT_TYPE_TOKEN:
        for_struct = True
    
    p[0] = AliasStatementNode(
        global_parsing_filename,for_struct,identifier_node,
        to_alias_to_string_node,line_number)
    
    
def p_StructList(p):
    '''
    StructList : StructList StructDefinition
               | Empty
    '''
    if len(p) == 2:
        struct_list_node = StructListNode(global_parsing_filename)
    else:
        struct_list_node = p[1]
        struct_definition_node = p[2]
        struct_list_node.add_struct_definition_node(struct_definition_node)
    p[0] = struct_list_node

def p_StructDefinition(p):
    '''
    StructDefinition : STRUCT_TYPE Identifier CURLY_LEFT StructBody CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    struct_name_node = p[2]
    # non-aliased version
    struct_body_node = p[4]
    p[0] = StructDefinitionNode(
        global_parsing_filename,struct_name_node,struct_body_node,line_number)

    
def p_StructBody(p):
    '''
    StructBody : StructBody DeclarationStatement SEMI_COLON
               | Empty
    '''
    if len(p) == 2:
        struct_body_node = StructBodyNode(global_parsing_filename)
    else:
        struct_body_node = p[1]
        declaration_statement_node = p[2]
        struct_body_node.add_struct_field(declaration_statement_node)
    p[0] = struct_body_node

    
def p_EndpointList(p):
    '''
    EndpointList : EndpointList EndpointDefinition
                 | EndpointList InterfaceDefinition
                 | EndpointDefinition
                 | InterfaceDefinition
                 | Empty
    '''
    if len(p) == 2:
        endpoint_list_node = EndpointListNode(
            global_parsing_filename)
        if is_empty(p[1]):
            pass
        elif is_endpoint_definition_node(p[1]):
            endpoint_definition_node = p[1]
            endpoint_list_node.append_endpoint_definition(
                endpoint_definition_node)
        else:
            interface_definition_node = p[1]
            endpoint_list_node.append_interface_definition(
                interface_definition_node)
    else:
        endpoint_list_node = p[1]

        if is_endpoint_definition_node(p[2]):
            endpoint_definition_node = p[2]
            endpoint_list_node.append_endpoint_definition(
                endpoint_definition_node)
        else:
            interface_definition_node = p[3]
            endpoint_list_node.append_interface_definition(
                interface_definition_node)
        
    p[0] = endpoint_list_node

def p_InterfaceDefinition(p):
    '''
    InterfaceDefinition : INTERFACE Identifier CURLY_LEFT InterfaceBody CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    interface_name_identifier_node = p[2]
    interface_body_node = p[4]
    p[0] = InterfaceDefinitionNode(
        global_parsing_filename,interface_name_identifier_node,
        interface_body_node,line_number)

def p_InterfaceBody(p):
    '''
    InterfaceBody : InterfaceMethodDeclaration InterfaceBody
                  | Empty
    '''
    if len(p) == 3:
        # method declaration statement
        interface_method_declaration_node = p[1]
        interface_body_node = p[2]
        interface_body_node.append_interface_method_declaration_node(
            interface_method_declaration_node)
    else:
        # empty statement
        interface_body_node = InterfaceBodyNode(global_parsing_filename)

    p[0] = interface_body_node
    
def p_InterfaceMethodDeclaration(p):
    '''
    InterfaceMethodDeclaration : MethodSignature SEMI_COLON
    '''
    method_signature_node = p[1]
    p[0] = InterfaceMethodDeclarationNode(
        global_parsing_filename,method_signature_node)
    
    
def p_EndpointDefinition(p):
    '''
    EndpointDefinition : ENDPOINT Identifier OptionalImplementsList CURLY_LEFT EndpointBody CURLY_RIGHT
                       | SERVICE Identifier OptionalImplementsList CURLY_LEFT EndpointBody CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    endpoint_name_identifier_node = p[2]
    optional_implements_list_node = p[3]
    endpoint_body_node = p[5]
   
    p[0] = EndpointDefinitionNode(
        global_parsing_filename,endpoint_name_identifier_node,
        optional_implements_list_node,endpoint_body_node,line_number)

def p_OptionalImplementsList(p):
    '''
    OptionalImplementsList : IMPLEMENTS OptionalImplementsList
                           | OptionalImplementsList COMMA VariableType
                           | VariableType
                           | Empty
    '''
    if len(p) == 2:
        implements_list_node = ImplementsListNode(global_parsing_filename,0)
        if not is_empty(p[1]):
            variable_type_node = p[1]
            implements_list_node = ImplementsListNode(
                global_parsing_filename,variable_type_node.line_number)
            implements_list_node.add_variable_type_node(variable_type_node)
    elif len(p) == 3:
        implements_list_node = p[2]
    elif len(p) == 4:
        implements_list_node = p[1]
        variable_type_node = p[3]
        implements_list_node.add_variable_type_node(variable_type_node)
    #### DEBUG
    else:
        raise InternalParseException(
            global_parsing_filename,p.lineno(0),
            'Incorrect number of tokens in optional implements list')
    #### END DEBUG
    p[0] = implements_list_node


def p_EndpointBody(p):
    '''
    EndpointBody : DeclarationStatement SEMI_COLON EndpointBody
                 | MethodDeclaration EndpointBody
                 | Empty
    '''
    if len(p) == 4:
        # variable declaration statement
        declaration_statement_node = p[1]
        endpoint_body_node = p[3]
        endpoint_body_node.prepend_variable_declaration_node(
            declaration_statement_node)
    elif len(p) == 3:
        # method declaration statement
        method_declaration_node = p[1]
        endpoint_body_node = p[2]
        endpoint_body_node.prepend_method_declaration_node(
            method_declaration_node)
    else:
        # empty statement
        endpoint_body_node = EndpointBodyNode(global_parsing_filename)

    p[0] = endpoint_body_node

def p_DeclarationStatement(p):
    '''
    DeclarationStatement : VariableType Identifier
                         | VariableType Identifier EQUALS Expression
    '''
    initializer_node = None
    
    if len(p) == 5:
        initializer_node = p[4]

    identifier_node = p[2]
    variable_type_node = p[1]
    p[0] = DeclarationStatementNode(
        global_parsing_filename,identifier_node,variable_type_node,
        initializer_node)

    
def p_MethodDeclaration(p):
    '''
    MethodDeclaration : MethodSignature CURLY_LEFT ScopeBody CURLY_RIGHT
                      | MethodSignature CURLY_LEFT CURLY_RIGHT
    '''
    method_signature_node = p[1]
    if len(p) == 4:
        # construct empty method body node
        method_body_node = ScopeBodyNode(
            global_parsing_filename,method_signature_node.line_number)
    if len(p) == 5:
        method_body_node = p[3]
    
    p[0] = MethodDeclarationNode(
        global_parsing_filename,method_signature_node,method_body_node)

    
def p_ScopeBody(p):
    '''
    ScopeBody : ScopeBody Statement
              | Statement
    '''
    if len(p) == 2:
        statement_node = p[1]
        scope_body_node = ScopeBodyNode(
            global_parsing_filename,statement_node.line_number)
    else:
        scope_body_node = p[1]
        statement_node = p[2]

    scope_body_node.append_statement_node(statement_node)
    p[0] = scope_body_node
    
    
def p_MethodSignature(p):
    '''
    MethodSignature : Identifier LEFT_PAREN MethodDeclarationArgs RIGHT_PAREN
                    | Identifier LEFT_PAREN MethodDeclarationArgs RIGHT_PAREN RETURNS VariableType
    '''
    method_name_identifier_node = p[1]
    method_declaration_args_node = p[3]
    returns_variable_type_node = None
    if len(p) == 7:
        returns_variable_type_node = p[6]

    p[0] = MethodSignatureNode(
        global_parsing_filename,method_name_identifier_node,
        method_declaration_args_node, returns_variable_type_node)

    
def p_MethodDeclarationArgs(p):
    '''
    MethodDeclarationArgs : MethodDeclarationArg
                          | MethodDeclarationArgs COMMA MethodDeclarationArg
                          | Empty
    '''
    if len(p) == 2:
        method_declaration_args = MethodDeclarationArgsNode(
            global_parsing_filename)
        second_arg = p[1]
        if second_arg is not None:
            method_declaration_args.append_method_declaration_arg(second_arg)
    else:
        method_declaration_args = p[1]
        method_declaration_arg = p[3]
        method_declaration_args.append_method_declaration_arg(
            method_declaration_arg)

    p[0] = method_declaration_args
        
    
def p_MethodDeclarationArg(p):
    '''
    MethodDeclarationArg : VariableType Identifier
    '''
    variable_type_node = p[1]
    identifier_node = p[2]
    p[0] = MethodDeclarationArgNode(
        global_parsing_filename,variable_type_node,identifier_node)
    

def p_MethodCall(p):
    '''
    MethodCall : Variable LEFT_PAREN MethodCallArgs RIGHT_PAREN
               | PRINT LEFT_PAREN MethodCallArgs RIGHT_PAREN
               | SPECULATE LEFT_PAREN MethodCallArgs RIGHT_PAREN
               | SPECULATE_CONTAINER_INTERNALS LEFT_PAREN MethodCallArgs RIGHT_PAREN
               | SPECULATE_ALL LEFT_PAREN RIGHT_PAREN
               | VERBATIM LEFT_PAREN MethodCallArgs RIGHT_PAREN
               | DYNAMIC_CAST LESS_THAN VariableType GREATER_THAN LEFT_PAREN MethodCallArgs RIGHT_PAREN
    '''
    if p[1] == PRINT_TYPE_TOKEN:
        line_number = p.lineno(1)
        method_args_node = p[3]
        p[0] = PrintCallNode(
            global_parsing_filename,method_args_node,line_number)
    elif p[1] == VERBATIM_TOKEN:
        line_number = p.lineno(1)
        method_args_node = p[3]
        p[0] = VerbatimCallNode(
            global_parsing_filename,method_args_node,line_number)
    elif p[1] == SPECULATE_TOKEN:
        line_number = p.lineno(1)
        method_args_node = p[3]
        p[0] = SpeculateCallNode(
            global_parsing_filename,method_args_node,line_number)
    elif p[1] == SPECULATE_CONTAINER_INTERNALS_TOKEN:
        line_number = p.lineno(1)
        method_args_node = p[3]
        p[0] = SpeculateContainerInternalsCallNode(
            global_parsing_filename,method_args_node,line_number)
    elif p[1] == SPECULATE_ALL_TOKEN:
        line_number = p.lineno(1)
        p[0] = SpeculateAllCallNode(
            global_parsing_filename,line_number)

    elif len(p) == 8:
        # dynamic cast
        line_number = p.lineno(1)
        to_variable_type_node = p[3]
        method_call_args_node = p[6]
        p[0] = DynamicCastNode(
            global_parsing_filename,to_variable_type_node,method_call_args_node,
            line_number)
        
    else:
        method_name_node = p[1]
        method_args_node = p[3]
        p[0] = MethodCallNode(
            global_parsing_filename,method_name_node,method_args_node)

def p_PartnerMethodCall(p):
    '''
    PartnerMethodCall : AT PARTNER DOT Identifier LEFT_PAREN MethodCallArgs RIGHT_PAREN
    '''
    line_number = p.lineno(1)
    method_name_node = p[4]
    method_call_args_node = p[6]
    p[0] = PartnerMethodCallNode(
        global_parsing_filename,method_name_node,method_call_args_node,
        line_number)


def p_MethodCallArgs(p):
    '''
    MethodCallArgs : Expression
                   | MethodCallArgs COMMA Expression
                   | Empty
    '''
    if len(p) == 4:
        expression_node = p[3]
        method_call_args_node = p[1]
        method_call_args_node.append_arg(expression_node)
    else:
        if is_empty(p[1]):
            method_call_args_node = MethodCallArgsNode(
                global_parsing_filename,0)
        else:
            expression_node = p[1]
            method_call_args_node = MethodCallArgsNode(
                global_parsing_filename,expression_node.line_number)
            method_call_args_node.append_arg(expression_node)
        
    p[0] = method_call_args_node
        
def p_Statement(p):
    '''
    Statement : Expression SEMI_COLON
              | ReturnStatement SEMI_COLON
              | DeclarationStatement SEMI_COLON
              | AssignmentStatement SEMI_COLON
              | ForStatement
              | ConditionStatement
              | ParallelStatement
              | ScopeStatement
              | AtomicallyStatement
    '''
    p[0] = p[1]

    
def p_AtomicallyStatement(p):
    '''
    AtomicallyStatement : ATOMICALLY ScopeStatement
    '''
    scope_node = p[2]
    p[0] = AtomicallyNode(global_parsing_filename,scope_node)
    
    
def p_ParallelStatement(p):
    '''
    ParallelStatement : PARALLEL LEFT_PAREN Expression COMMA Expression RIGHT_PAREN 
    '''
    line_number = p.lineno(1)
    to_iter_over_node = p[3]
    lambda_node = p[5]
    p[0] = ParallelNode(
        global_parsing_filename,to_iter_over_node,lambda_node,line_number)


def p_LenExpression(p):
    '''
    LenExpression : LEN LEFT_PAREN Expression RIGHT_PAREN
    '''
    len_argument_node = p[3]
    p[0] = LenNode(global_parsing_filename,len_argument_node)

    
def p_RangeExpression(p):
    '''
    RangeExpression : RANGE LEFT_PAREN Expression COMMA Expression COMMA Expression RIGHT_PAREN
                    | RANGE LEFT_PAREN Expression COMMA Expression RIGHT_PAREN
    '''
    start_expression_node = p[3]
    line_number = p.lineno(1)
    if len(p) == 9:
        increment_expression_node = p[5]
        end_expression_node = p[7]
    else:
        increment_expression_node = NumberLiteralNode(
            global_parsing_filename,1,line_number)
        end_expression_node = p[5]

    range_expression_node = RangeExpressionNode(
        global_parsing_filename,start_expression_node,increment_expression_node,
        end_expression_node,line_number)

    p[0] = range_expression_node
    
    
def p_ConditionStatement(p):
    '''
    ConditionStatement : IfStatement ElseIfStatements ElseStatement
    '''
    if_node = p[1]
    elifs_node = p[2]
    else_node = p[3]
    
    condition_statement = ConditionNode(
        global_parsing_filename,if_node,elifs_node,else_node)
    p[0] = condition_statement
    
    
def p_IfStatement(p):
    '''
    IfStatement : IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''
    predicate_node = p[3]
    if_body_node = p[5]
    line_number = p.lineno(1)
    p[0] = IfNode(
        global_parsing_filename,predicate_node,if_body_node,line_number)

def p_ElseIfStatements(p):
    '''
    ElseIfStatements : ElseIfStatements ElseIfStatement
                     | Empty
    '''
    if len(p) == 2:
        else_if_nodes = ElseIfNodes(global_parsing_filename)
    else:
        else_if_nodes = p[1]
        else_if_statement = p[2]
        else_if_nodes.append_else_if(else_if_statement)
    p[0] = else_if_nodes

    
def p_ElseIfStatement(p):
    '''
    ElseIfStatement : ELSE_IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''
    line_number = p.lineno(1)
    # using if node for else if node
    predicate_node = p[3]
    body_node = p[5]
    else_if_node = ElifNode(
        global_parsing_filename,predicate_node,body_node,line_number)
    p[0] = else_if_node

def p_ElseStatement(p):
    '''
    ElseStatement : ELSE Statement
                  | Empty
    '''
    if len(p) == 2:
        else_node = ElseNode(global_parsing_filename,0)
    else:
        line_number = p.lineno(1)
        body_node = p[2]
        else_node = ElseNode(global_parsing_filename,line_number)
        else_node.add_else_body_node(body_node)
    p[0] = else_node
    
    
def p_ForStatement(p):
    '''
    ForStatement : FOR LEFT_PAREN VariableType Identifier IN Expression RIGHT_PAREN Statement
    '''
    line_number = p.lineno(1)
    # declaring variable locally
    variable_type_node = p[3]
    variable_node = p[4]
    in_what_node = p[6]
    statement_node = p[8]
    p[0] = ForNode(
        global_parsing_filename,variable_type_node,variable_node,in_what_node,
        statement_node,line_number)

def p_ScopeStatement(p):
    '''
    ScopeStatement : CURLY_LEFT ScopeBody CURLY_RIGHT
                   | CURLY_LEFT CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    if len(p) == 3:
        scope_body_node = ScopeBodyNode(global_parsing_filename,line_number)
    else:
        scope_body_node = p[2]
    p[0] = ScopeNode(global_parsing_filename,scope_body_node)
        
    
def p_AssignmentStatement(p):
    '''
    AssignmentStatement : Variable EQUALS Expression
    '''
    lhs_node = p[1]
    rhs_node = p[3]
    p[0] = AssignmentNode(global_parsing_filename,lhs_node,rhs_node)

    
def p_Variable(p):
    '''
    Variable : Identifier
             | Variable LEFT_BRACKET Expression RIGHT_BRACKET
             | Variable DOT Identifier
    '''

    if len(p) == 2:
        # Identifier
        identifier_node = p[1]
        p[0] = identifier_node
        
    elif len(p) == 5:
        # bracket expression
        
        # variable_node is either an identifier node, bracket node, or
        # dot node        
        variable_node = p[1]
        inner_bracket_node = p[3]
        bracket_node = BracketNode(
            global_parsing_filename,variable_node,inner_bracket_node)
        p[0] = bracket_node
        
    elif len(p) == 4:
        # dot expression
        
        # variable_node is either an identifier node, bracket node, or
        # dot node
        variable_node = p[1]
        identifier_node = p[3]
        dot_node = DotNode(
            global_parsing_filename,variable_node,identifier_node)
        p[0] = dot_node
    #### DEBUG
    else:
        raise InternalParseException(
            global_parsing_filename,p.lineno(0),
            'Incorrect number of tokens in variable')
    #### END DEBUG
    
def p_ReturnStatement(p):
    '''
    ReturnStatement : RETURN_OPERATOR
                    | RETURN_OPERATOR Expression
    '''
    line_number = p.lineno(1)
    return_node = ReturnNode(global_parsing_filename,line_number)
    if len(p) == 3:
        what_to_return_node = p[2]
        return_node.add_return_expression_node(what_to_return_node)
    p[0] = return_node
    
def p_Expression(p):
    '''
    Expression : OrExpression
    '''
    p[0] = p[1]

def p_OrExpression(p):
    '''
    OrExpression : OrExpression OR AndExpression
                 | AndExpression
    '''
    production_rule_for_binary_operator(p)

def p_AndExpression(p):
    '''
    AndExpression : AndExpression AND InNotInExpression
                  | InNotInExpression
    '''
    production_rule_for_binary_operator(p)

def p_InNotInExpression (p):
    '''
    InNotInExpression : InNotInExpression IN EqualsNotEqualsExpression
                      | InNotInExpression NOT IN EqualsNotEqualsExpression
                      | EqualsNotEqualsExpression
    '''
    if len(p) == 2:
        etc_expression = p[1]
        p[0] = etc_expression
    else:
        if len(p) == 4:
            lhs_expression_node = p[1]
            rhs_expression_node = p[3]
            operator = p[2]
        else:
            lhs_expression_node = p[1]
            rhs_expression_node = p[4]
            operator = 'not in'

        p[0] = create_binary_expression_node(
            operator,lhs_expression_node,rhs_expression_node)
        
    
def p_EqualsNotEqualsExpression(p):
    '''
    EqualsNotEqualsExpression : EqualsNotEqualsExpression BOOL_EQUALS GreaterThanLessThanExpression
                              | EqualsNotEqualsExpression BOOL_NOT_EQUALS GreaterThanLessThanExpression
                              | GreaterThanLessThanExpression
    '''
    production_rule_for_binary_operator(p)
    
def p_GreaterThanLessThanExpression(p):
    '''
    GreaterThanLessThanExpression : GreaterThanLessThanExpression GREATER_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression GREATER_THAN_EQ PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN_EQ PlusMinusExpression
                                  | PlusMinusExpression
    '''
    production_rule_for_binary_operator(p)
    
def p_PlusMinusExpression(p):
    '''
    PlusMinusExpression : PlusMinusExpression PLUS MultDivExpression
                        | PlusMinusExpression MINUS MultDivExpression
                        | MultDivExpression
    '''
    production_rule_for_binary_operator(p)

    
def p_MultDivExpression(p):
    '''
    MultDivExpression : MultDivExpression MULTIPLY NotExpression
                      | MultDivExpression DIVIDE NotExpression
                      | NotExpression
    '''
    production_rule_for_binary_operator(p)
    
def production_rule_for_binary_operator(p):
    '''
    Generally has the form
      rule : rule operator etc
           | etc
    '''
    if len(p) == 2:
        etc_expression = p[1]
        p[0] = etc_expression
    else:
        lhs_expression_node = p[1]
        rhs_expression_node = p[3]
        operator = p[2]

        p[0] = create_binary_expression_node(
            operator,lhs_expression_node,rhs_expression_node)

    
def p_NotExpression(p):
    '''
    NotExpression : NOT Term
                  | Term
    '''
    p[0] = p[1]
    if len(p) == 3:
        term_node = p[2]
        not_node = NotNode(global_parsing_filename,term_node)
        p[0] = not_node
        
def p_Term(p):
    '''
    Term : Variable
         | MethodCall
         | PartnerMethodCall
         | Break
         | Continue
         | Number
         | String
         | Boolean
         | Self
         | Null
         | LEFT_PAREN Expression RIGHT_PAREN
         | RangeExpression
         | LenExpression
    '''
    term_node = p[1]
    if len(p) == 4:
        term_node = p[2]

    p[0] = term_node

def p_Break(p):
    '''
    Break : BREAK
    '''
    line_number = p.lineno(1)
    p[0] = BreakNode(global_parsing_filename,line_number)

def p_Continue(p):
    '''
    Continue : CONTINUE
    '''
    line_number = p.lineno(1)
    p[0] = ContinueNode(global_parsing_filename,line_number)
    
    
def p_Self(p):
    '''
    Self : SELF
    '''
    line_number = p.lineno(1)
    p[0] = SelfNode(global_parsing_filename,line_number)
    
def p_Number(p):
    '''
    Number : NUMBER
           | MINUS NUMBER
    '''
    if len(p) == 2:
        number_literal = float(p[1])
    else:
        number_literal = -float(p[2])
    line_number = p.lineno(1)
    p[0] = NumberLiteralNode(
        global_parsing_filename,number_literal,line_number)
    
def p_String(p):
    '''
    String : SINGLE_LINE_STRING
    '''
    text_literal = p[1]
    line_number = p.lineno(1)
    p[0] = TextLiteralNode(global_parsing_filename,text_literal,line_number)
    
def p_Boolean(p):
    '''
    Boolean : TRUE
            | FALSE
    '''
    boolean_literal = p[1] == 'True'
    line_number = p.lineno(1)
    p[0] = TrueFalseLiteralNode(
        global_parsing_filename,boolean_literal,line_number)

def p_Null(p):
    '''
    Null : NULL
    '''
    line_number = p.lineno(1)
    p[0] = NullLiteralNode(global_parsing_filename,line_number)
    
    
def p_VariableType(p):
    '''
    VariableType : BOOL_TYPE
                 | NUMBER_TYPE
                 | STRING_TYPE

                 | TVAR BOOL_TYPE
                 | TVAR NUMBER_TYPE
                 | TVAR STRING_TYPE

                 | MAP_TYPE LEFT_PAREN FROM COLON VariableType COMMA TO COLON VariableType RIGHT_PAREN
                 | TVAR MAP_TYPE LEFT_PAREN FROM COLON VariableType COMMA TO COLON VariableType RIGHT_PAREN

                 | LIST_TYPE LEFT_PAREN ELEMENT COLON VariableType RIGHT_PAREN
                 | TVAR LIST_TYPE LEFT_PAREN ELEMENT COLON VariableType RIGHT_PAREN
                 
                 | STRUCT_TYPE Identifier
                 | TVAR STRUCT_TYPE Identifier

                 | ENDPOINT Identifier
                 | TVAR ENDPOINT Identifier

                 | SERVICE Identifier
                 | TVAR SERVICE Identifier

                 | INTERFACE Identifier
                 | TVAR INTERFACE Identifier
                 
                 | SERVICE_FACTORY
                 | TVAR SERVICE_FACTORY
    '''
    if len(p) >= 11:
        # It's a map
        if len(p) == 11:
            # non tvar map type
            is_tvar = False
            from_type_index = 5
            to_type_index = 9
        elif len(p) == 12:
            is_tvar = True
            from_type_index = 6
            to_type_index = 10
        else:
            raise InternalParseException(
                global_parsing_filename,p.lineno(0),
                'Incorrect number of tokens for map variable')

        from_type_node = p[from_type_index]
        to_type_node = p[to_type_index]
        line_number = p.lineno(0)
        p[0] = MapVariableTypeNode(
            global_parsing_filename,from_type_node,to_type_node,
            is_tvar,line_number)
    elif len(p) in [7,8]:
        # it's a list type
        line_number = p.lineno(1)
        is_tvar = False
        list_element_type_index = 5
        if len(p) == 8:
            is_tvar = True
            list_element_type_index = 6
        list_element_type_node = p[list_element_type_index]
        p[0] = ListVariableTypeNode(
            global_parsing_filename,list_element_type_node,is_tvar,line_number)
    elif (
        (p[1] == STRUCT_TYPE_TOKEN) or
        ((len(p) >= 3) and (p[2] == STRUCT_TYPE_TOKEN))):
        # emitting a struct type
        is_tvar = False
        struct_name_node_index = 2
        if len(p) == 4:
            # struct is a tvar
            is_tvar = True
            struct_name_node_index = 3
        line_number = p.lineno(1)
        struct_name_node = p[struct_name_node_index]
        p[0] = StructVariableTypeNode(
            global_parsing_filename,struct_name_node,is_tvar,line_number)

    elif (((p[1] == ENDPOINT_TOKEN) or
           ((len(p) >= 3) and (p[2] == ENDPOINT_TOKEN)))
          or 
          ((p[1] == SERVICE_TOKEN) or
           ((len(p) >= 3) and (p[2] == SERVICE_TOKEN)))
          or
          ((p[1] == INTERFACE_TOKEN) or
           ((len(p) >= 3) and (p[2] == INTERFACE_TOKEN)))):

        # emitting an endpoint type
        is_tvar = False
        endpoint_name_node_index = 2
        if len(p) == 4:
            # endpoint is a tvar
            is_tvar = True
            endpoint_name_node_index = 3
        line_number = p.lineno(1)
        endpoint_name_node = p[endpoint_name_node_index]
        p[0] = EndpointVariableTypeNode(
            global_parsing_filename,endpoint_name_node,is_tvar,line_number)

    elif (
        (p[1] == SERVICE_FACTORY_TOKEN) or
        ((len(p) >= 3) and (p[2] == SERVICE_FACTORY_TOKEN))):
        # emitting a service factory type
        is_tvar = False
        if len(p) == 3:
            # service factory is a tvar
            is_tvar = True
        line_number = p.lineno(1)
        p[0] = ServiceFactoryVariableTypeNode(
            global_parsing_filename,is_tvar,line_number)
        
    else:
        # basic type or tvar type
        basic_type_index = 1
        is_tvar = False
        if len(p) == 3:
            basic_type_index = 2
            is_tvar = True

        basic_type = p[basic_type_index]
        line_number = p.lineno(basic_type_index)
        p[0] = BasicTypeNode(
            global_parsing_filename,basic_type,is_tvar,line_number)


def p_Identifier(p):
    '''
    Identifier : IDENTIFIER
    '''
    line_number = p.lineno(1)
    value = p[1]
    p[0] = IdentifierNode(global_parsing_filename,value,line_number)
    
    
def p_error(p):
    raise ParseException(
        global_parsing_filename, p.lineno,
        'Error parsing token "%s".' % p.value)

    
def p_Empty(p):
    '''
    Empty :
    '''
    p[0] = None

def is_empty(to_test):
    return to_test is None

def is_endpoint_definition_node(to_test):
    return to_test.label == ast_labels.ENDPOINT_DEFINITION_STATEMENT

def construct_parser(suppress_warnings,parsing_filename):
    global lexer
    lexer = construct_lexer(parsing_filename)

    global global_parsing_filename
    global_parsing_filename = parsing_filename
    
    if suppress_warnings:
        returner = yacc.yacc(errorlog=yacc.NullLogger())
    else:
        returner = yacc.yacc()

    return returner;
