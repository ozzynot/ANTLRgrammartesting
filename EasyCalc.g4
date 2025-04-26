grammar EasyCalc;

program : declar* stmt* '$$';

declar  : type=('bool' | 'int' | 'real') ID ';';

stmt    : ID ':=' expr ';'                      # AssignStmt
        | 'read' ID ';'                         # ReadStmt
        | 'write' expr ';'                      # WriteStmt
        ;

expr    : LIT                                   # LitExpr
        | ID                                    # IdExpr
        | '(' expr ')'                          # ParenExpr
        | op=('to_int'|'to_real') '(' expr ')'  # ToExpr        // Can generate unidentified for bool
        | expr op=('*' | '/') expr              # MulDivExpr    // Can generate +, - , *, / unidentified for bool OR type clash
        | expr op=('+' | '-') expr              # AddSubExpr    // Same as MulDiv
        | expr op=('<' | '>') expr              # LessGrtrExpr  // Can generate <, > unidentified for bool OR type clash
        | expr op='==' expr                     # EqualExpr     // Can generate type clash
        | expr op='and' expr                    # AndExpr       // Can generate unidentified for real and int, both operands are bool
        | expr op='or' expr                     # OrExpr        // Same as and
        | 'if' expr 'then' expr 'else' expr     # IfExpr        // Can generate undefined in first expr for real and int
        ;                                                       // in second expr, type clash with third
                                                                // in third expr, type clash with second
DSTOP   : '$$';
SSTOP   : ';';
BOOL    : 'bool';
INT     : 'int';
REAL    : 'real';
ASSIGN  : ':=';
READ    : 'read';
WRITE   : 'write';
LPAREN  : '(';
RRAPEN  : ')';
TINT    : 'to_int';
TREAL   : 'to_real';
MUL     : '*';
DIV     : '/';
ADD     : '+';
SUB     : '-';
LESS    : '<';
GRTR    : '>';
EQUAL   : '==';
AND     : 'and';
OR      : 'or';
IF      : 'if';
THEN    : 'then';
ELSE    : 'else';

LIT     : [0-9]+ | ([0-9]* ('.' [0-9] | [0-9] '.') [0-9]*) | 'true' | 'false';
ID      : [a-zA-Z][a-zA-Z0-9_]*;

WS      : [ \t\r\n]+ -> skip;