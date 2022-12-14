grammar Anqur;

program : decl+;
decl
 : 'def' ID param* ':' expr fnBody # fnDecl
 | 'print' param* ':' expr ARROW2 expr # printDecl
 | 'data' ID param* consDecl* # dataDecl
 ;
fnBody : ARROW2 expr | clause*;
pattern : ID | '(' ID pattern* ')';
clause : '|' pattern+ ARROW2 expr;
consDecl : '|' ID param*;
param : '(' ID+ ':' expr ')';
expr
 // Elimination lures
 : expr expr # two
 | expr '.1' # fst
 | expr '.2' # snd

 // Type formers
 | UNIV # keyword
 | <assoc=right> expr ARROW expr # simpFun
 | <assoc=right> expr TIMES expr # simpTup
 | PI param ARROW expr # pi
 | SIG param TIMES expr # sig

 // Introduction lures
 | LAM ID+ '.' expr # lam
 | LPAIR expr ',' expr RPAIR # pair

 // Others
 | ID # ref
 | '(' expr ')' # paren
 ;

LPAIR : '<<';
RPAIR : '>>';
ARROW : '->' | '\u2192';
ARROW2 : '=>' | '\u21D2';
TIMES : '**' | '\u00D7';
SIG : 'Sig' | '\u03A3';
LAM : '\\' | '\u03BB';
PI : 'Pi' | '\u03A0';
RIGHT : '1';
LEFT : '0';
UNIV : 'U' | 'Type';

// Below are copy-and-paste from Aya. Plagiarism!! LOL

// identifier
fragment AYA_SIMPLE_LETTER : [~!@#$%^&*+=<>?/|[\u005Da-zA-Z_\u2200-\u22FF];
fragment AYA_UNICODE : [\u0080-\uFEFE] | [\uFF00-\u{10FFFF}]; // exclude U+FEFF which is a truly invisible char
fragment AYA_LETTER : AYA_SIMPLE_LETTER | AYA_UNICODE;
fragment AYA_LETTER_FOLLOW : AYA_LETTER | [0-9'-];
REPL_COMMAND : ':' AYA_LETTER_FOLLOW+;
ID : AYA_LETTER AYA_LETTER_FOLLOW* | '-' AYA_LETTER AYA_LETTER_FOLLOW*;

// whitespaces
WS : [ \t\r\n]+ -> channel(HIDDEN);
fragment COMMENT_CONTENT : ~[\r\n]*;
DOC_COMMENT : '--|' COMMENT_CONTENT;
LINE_COMMENT : '--' COMMENT_CONTENT -> channel(HIDDEN);
COMMENT : '{-' (COMMENT|.)*? '-}' -> channel(HIDDEN);

// avoid token recognition error in REPL
ERROR_CHAR : .;
