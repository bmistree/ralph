
class CompilerException(Exception):
    def __init__(self,line_num,error_text):
        self.line_num = line_num
        self.error_text = error_text
    
