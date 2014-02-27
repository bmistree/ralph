
class PreprocessorNode(object):
    pass

class IdentifierNode(PreprocessorNode):
    def __init__(self,filename,line_number,value):
        self.filename = filename
        self.line_number = line_number
        self.value = value
    def get_text(self):
        return self.value
    
class TextLiteralNode(IdentifierNode):
    '''
    Currently, has the same functionality as IdentifierNode.
    '''
    pass

class PreprocessorStatementListNode(PreprocessorNode):
    def __init__(self,filename):
        self.filename = filename
        self.statement_list = []
    def add_statement_node(self,preprocessor_node):
        self.statement_list.append(preprocessor_node)

class IncludeStatementNode(PreprocessorNode):
    def __init__(self,filename,line_number,where_to_include_from):
        self.filename = filename
        self.line_number = line_number
        self.where_to_include_from = where_to_include_from

class StructAliasStatementNode(PreprocessorNode):
    def __init__(self,filename,line_number,identifier_text,alias_to_text):
        self.filename = filename
        self.line_number = line_number
        self.identifier_text = identifier_text
        self.alias_to_text = alias_to_text

class EndpointAliasStatementNode(PreprocessorNode):
    def __init__(self,filename,line_number,identifier_text,alias_to_text):
        self.filename = filename
        self.line_number = line_number
        self.identifier_text = identifier_text
        self.alias_to_text = alias_to_text
