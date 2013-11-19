

class Type(object):
    BASIC_TYPE_FIELD = 'basic_type'
    IS_TVAR_FIELD = 'is_tvar'
    
    def __init__(self,basic_type,is_tvar):
        self._t_dict = {
            Type.BASIC_TYPE_FIELD: basic_type,
            Type.IS_TVAR_FIELD: is_tvar
            }

    def __eq__(self,other_type):
        return self._t_dict == other_type._t_dict

