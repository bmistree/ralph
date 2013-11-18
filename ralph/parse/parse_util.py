from ralph.common.compiler_exceptions import CompilerException

class InternalParseException(CompilerException):
    def __init__(self,msg):
        self.msg = msg
    def __str__(self):
        return self.msg


class ParseException(CompilerException):
    def __init__(self,line_number,msg):
        self.line_number = line_number
        self.msg = msg
    def __str__(self):
        return 'Error on line %i:%s' % (self.line_number,self.msg)

