grammar Formula;

// Parser rules
formula
    : expression EOF
    ;

expression
    : additive
    ;

additive
    : multiplicative ((PLUS | MINUS) multiplicative)*
    ;

multiplicative
    : unary ((MUL | DIV) unary)*
    ;

unary
    : (MINUS | PLUS)? power
    ;

power
    : primary (POWER primary)*
    ;

primary
    : atom
    | LPAREN expression RPAREN
    | functionCall
    | cellReference
    ;

functionCall
    : IDENTIFIER LPAREN argList? RPAREN
    ;

argList
    : expression (COMMA expression)*
    ;

cellReference
    : CELL_REF
    | CELL_RANGE
    ;

atom
    : NUMBER
    | STRING
    | BOOLEAN
    ;

// Lexer rules
BOOLEAN
    : 'TRUE' | 'FALSE'
    ;

NUMBER
    : INTEGER
    | FLOAT
    ;

fragment INTEGER
    : DIGIT+
    ;

fragment FLOAT
    : DIGIT+ '.' DIGIT*
    | '.' DIGIT+
    ;

fragment DIGIT
    : [0-9]
    ;

STRING
    : '"' (~["\r\n])* '"'
    ;

CELL_RANGE
    : CELL_REF ':' CELL_REF
    ;

CELL_REF
    : [A-Z]+ [0-9]+
    ;

IDENTIFIER
    : [A-Za-z_][A-Za-z0-9_]*
    ;

LPAREN
    : '('
    ;

RPAREN
    : ')'
    ;

PLUS
    : '+'
    ;

MINUS
    : '-'
    ;

MUL
    : '*'
    ;

DIV
    : '/'
    ;

POWER
    : '^'
    ;

COMMA
    : ','
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
