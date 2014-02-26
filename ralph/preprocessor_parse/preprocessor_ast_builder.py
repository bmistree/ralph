from ralph.preprocessor_lex.preprocessor_lex import tokens,construct_lexer
from ralph.preprocessor_lex.preprocessor_lex import _STRUCT_TOKEN
import deps.ply.yacc as yacc
from ralph.parse.parse_util import InternalParseException,ParseException
from ralph.preprocessor_parse.preprocessor_node import *

#note: global variable used by yacc.  Without this set, otherwise,
#starts at first rule.
start = 'PreprocessorRootStatement'
#need to have something named lexer for parser to chew into
lexer = None

def p_PreprocessorRootStatement(p):
    '''
    PreprocessorRootStatement : PreprocessorStatementList
    '''
    p[0] = p[1]

def p_PreprocessorStatementList(p):
    '''
    PreprocessorStatementList : PreprocessorStatementList PreprocessorStatement
                              | Empty
    '''
    if len(p) == 2:
        preprocessor_statement_list_node = PreprocessorStatementListNode()
    else:
        preprocessor_statement_list_node = p[1]
        preprocessor_statement_node = p[2]
        preprocessor_statement_list_node.add_statement_node(
            preprocessor_statement_node)
        
    p[0] = preprocessor_statement_list_node

def p_PreprocessorStatement(p):
    '''
    PreprocessorStatement : IncludeStatement
                          | AliasStatement
    '''
    p[0] = p[1]

def p_IncludeStatement(p):
    '''
    IncludeStatement : INCLUDE String
    '''
    line_number = p.lineno(1)
    where_to_include_from_node = p[2]
    p[0] = IncludeStatementNode(line_number,where_to_include_from_node.get_text())

def p_AliasStatement(p):
    '''
    AliasStatement : ALIAS STRUCT Identifier AS String
                   | ALIAS ENDPOINT Identifier AS String
                   | ALIAS SERVICE Identifier AS String
    '''
    line_number = p.lineno(1)
    identifier_text = p[3].get_text()
    alias_to_text = p[5].get_text()
    if p[2] == _STRUCT_TOKEN:
        alias_statement_node = StructAliasStatementNode(
            line_number,identifier_text,alias_to_text)
    else:
        alias_statement_node = EndpointAliasStatementNode(
            line_number,identifier_text,alias_to_text)
    
    p[0] = alias_statement_node

def p_Identifier(p):
    '''
    Identifier : IDENTIFIER
    '''
    line_number = p.lineno(1)
    value = p[1]
    p[0] = IdentifierNode(line_number,value)
    
def p_String(p):
    '''
    String : SINGLE_LINE_STRING
    '''
    text_literal = p[1]
    line_number = p.lineno(1)
    p[0] = TextLiteralNode(line_number,text_literal)

def p_Empty(p):
    '''
    Empty :
    '''
    p[0] = None

def is_empty(to_test):
    return to_test is None
    
def construct_parser(suppress_warnings):
    global lexer
    lexer = construct_lexer()
    
    if suppress_warnings:
        returner = yacc.yacc(errorlog=yacc.NullLogger())
    else:
        returner = yacc.yacc()

    return returner;
