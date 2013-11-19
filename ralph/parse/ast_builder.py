#!/usr/bin/python

from ralph.lex.ralph_lex import tokens,construct_lexer
import deps.ply.ply.yacc as yacc
from ralph.parse.ast_node import *

#note: global variable used by yacc.  Without this set, otherwise,
#starts at first rule.
start = 'RootStatement'
#need to have something named lexer for parser to chew into
lexer = None


def p_RootStatement(p):
    '''
    RootStatement : EndpointList
    '''
    endpoint_list_node = p[1]
    p[0] = RootStatementNode(endpoint_list_node)
    
    
def p_EndpointList(p):
    '''
    EndpointList : EndpointList EndpointDefinition 
                 | EndpointDefinition
    '''
    if len(p) == 2:
        endpoint_definition_node = p[1]
        endpoint_list_node = EndpointListNode(endpoint_definition_node)
    else:
        endpoint_list_node = p[1]
        endpoint_definition_node = p[2]
        endpoint_list_node.prepend_endpoint_definition(
            endpoint_definition_node)
        
    p[0] = endpoint_list_node

    
def p_EndpointDefinition(p):
    '''
    EndpointDefinition : ENDPOINT Identifier CURLY_LEFT EndpointBody CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    endpoint_name_identifier_node = p[2]
    endpoint_body_node = p[4]
    
    p[0] = EndpointDefinitionNode(
        endpoint_name_identifier_node,endpoint_body_node,line_number)


def p_EndpointBody(p):
    '''
    EndpointBody : DeclarationStatement SEMI_COLON EndpointBody
                 | FunctionDeclaration EndpointBody
                 | Empty
    '''
    if len(p) == 4:
        # variable declaration statement
        declaration_statement_node = p[1]
        endpoint_body_node = p[3]
        endpoint_body_node.prepend_variable_declaration_node(
            declaration_statement_node)
    elif len(p) == 3:
        # function declaration statement
        function_declaration_node = p[1]
        endpoint_body_node = p[2]
        endpoint_body_node.prepend_function_declaration_node(
            function_declaration_node)
    else:
        # empty statement
        endpoint_body_node = EndpointBodyNode()

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
        identifier_node,variable_type_node,initializer_node)

    
def p_FunctionDeclaration(p):
    '''
    FunctionDeclaration : FunctionSignature CURLY_LEFT FunctionBody CURLY_RIGHT
                        | FunctionSignature CURLY_LEFT CURLY_RIGHT
    '''

def p_FunctionBody(p):
    '''
    FunctionBody : FunctionBody Statement
                 | Statement
    '''
    
def p_FunctionSignature(p):
    '''
    FunctionSignature : Identifier LEFT_PAREN FunctionDeclarationArgs RIGHT_PAREN
                      | Identifier LEFT_PAREN FunctionDeclarationArgs RIGHT_PAREN RETURNS VariableType
    '''
    
def p_FunctionDeclarationArgs(p):
    '''
    FunctionDeclarationArgs : FunctionDeclarationArg
                            | FunctionDeclarationArgs COMMA FunctionDeclarationArg
                            | Empty
    '''

def p_FunctionDeclarationArg(p):
    '''
    FunctionDeclarationArg : VariableType Identifier
    '''

def p_FunctionCall(p):
    '''
    FunctionCall : Variable LEFT_PAREN FunctionCallArgs RIGHT_PAREN
    '''
    
def p_FunctionCallArgs(p):
    '''
    FunctionCallArgs : Expression
                     | FunctionCallArgs COMMA Expression
                     | Empty
    '''
    
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
def p_AtomicallyStatement(p):
    '''
    AtomicallyStatement : ATOMICALLY ScopeStatement
    '''
    
def p_ParallelStatement(p):
    '''
    ParallelStatement : PARALLEL LEFT_PAREN Expression COMMA Expression RIGHT_PAREN 
    '''

def p_LenExpression(p):
    '''
    LenExpression : LEN LEFT_PAREN Expression RIGHT_PAREN
    '''
    
def p_RangeExpression(p):
    '''
    RangeExpression : RANGE LEFT_PAREN Expression COMMA Expression COMMA Expression RIGHT_PAREN
                    | RANGE LEFT_PAREN Expression COMMA Expression RIGHT_PAREN
    '''
    
def p_ConditionStatement(p):
    '''
    ConditionStatement : IfStatement ElseIfStatements ElseStatement
    '''

def p_IfStatement(p):
    '''
    IfStatement : IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''

def p_ElseIfStatements(p):
    '''
    ElseIfStatements : ElseIfStatements ElseIfStatement
                     | Empty
    '''
def p_ElseIfStatement(p):
    '''
    ElseIfStatement : ELSE_IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''

def p_ElseStatement(p):
    '''
    ElseStatement : ELSE Statement
                  | Empty
    '''
    
    
def p_ForStatement(p):
    '''
    ForStatement : FOR LEFT_PAREN VariableType Identifier IN Expression RIGHT_PAREN Statement
                 | FOR LEFT_PAREN Variable IN Expression RIGHT_PAREN Statement
    '''

def p_ScopeStatement(p):
    '''
    ScopeStatement : CURLY_LEFT Statement CURLY_RIGHT
                   | CURLY_LEFT CURLY_RIGHT
    '''
    
def p_AssignmentStatement(p):
    '''
    AssignmentStatement : Variable EQUALS Expression
    '''

def p_Variable(p):
    '''
    Variable : Identifier
             | Variable LEFT_BRACKET Expression RIGHT_BRACKET
             | Variable DOT Identifier
    '''
    
def p_ReturnStatement(p):
    '''
    ReturnStatement : RETURN_OPERATOR
                    | RETURN_OPERATOR Expression
    '''

def p_Expression(p):
    '''
    Expression : OrExpression
    '''
    
def p_OrExpression(p):
    '''
    OrExpression : OrExpression OR AndExpression
                 | AndExpression
    '''
    
def p_AndExpression(p):
    '''
    AndExpression : AndExpression AND InNotInExpression
                  | InNotInExpression
    '''
def p_InNotInExpression (p):
    '''
    InNotInExpression : InNotInExpression IN EqualsNotEqualsExpression
                      | InNotInExpression NOT IN EqualsNotEqualsExpression
                      | EqualsNotEqualsExpression
    '''
    
def p_EqualsNotEqualsExpression(p):
    '''
    EqualsNotEqualsExpression : EqualsNotEqualsExpression BOOL_EQUALS GreaterThanLessThanExpression
                              | EqualsNotEqualsExpression BOOL_NOT_EQUALS GreaterThanLessThanExpression
                              | GreaterThanLessThanExpression
    '''

def p_GreaterThanLessThanExpression(p):
    '''
    GreaterThanLessThanExpression : GreaterThanLessThanExpression GREATER_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression GREATER_THAN_EQ PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN_EQ PlusMinusExpression
                                  | PlusMinusExpression
    '''

    
def p_PlusMinusExpression(p):
    '''
    PlusMinusExpression : PlusMinusExpression PLUS MultDivExpression
                        | PlusMinusExpression MINUS MultDivExpression
                        | MultDivExpression
    '''
def p_MultDivExpression(p):
    '''
    MultDivExpression : MultDivExpression MULTIPLY NotExpression
                      | MultDivExpression DIVIDE NotExpression
                      | NotExpression
    '''    
def p_NotExpression(p):
    '''
    NotExpression : NOT Term
                  | Term
    '''
    
def p_Term(p):
    '''
    Term : Variable
         | FunctionCall
         | Number
         | String
         | Boolean
         | LEFT_PAREN Expression RIGHT_PAREN
         | RangeExpression
         | LenExpression
    '''
def p_Number(p):
    '''
    Number : NUMBER
    '''
def p_String(p):
    '''
    String : SINGLE_LINE_STRING
    '''
def p_Boolean(p):
    '''
    Boolean : TRUE
            | FALSE
    '''
    
def p_VariableType(p):
    '''
    VariableType : BOOL_TYPE
                 | NUMBER_TYPE
                 | STRING_TYPE

                 | TVAR BOOL_TYPE
                 | TVAR NUMBER_TYPE
                 | TVAR STRING_TYPE
    '''
    basic_type_index = 1
    is_tvar = False
    if len(p) == 3:
        basic_type_index = 2
        is_tvar = True

    basic_type = p[basic_type_index]
    line_number = p.lineno(basic_type_index)
    p[0] = VariableTypeNode(basic_type,is_tvar,line_number)


def p_Identifier(p):
    '''
    Identifier : IDENTIFIER
    '''
    line_number = p.lineno(1)
    value = p[1]
    p[0] = IdentifierNode(value,line_number)
    

def p_Empty(p):
    '''
    Empty :
    '''
    
    
def construct_parser(suppress_warnings):
    global lexer
    lexer = construct_lexer()
    
    if suppress_warnings:
        returner = yacc.yacc(errorlog=yacc.NullLogger())
    else:
        returner = yacc.yacc()

    return returner;
