grammar FlightDSL;



flightPlan  : flight EOF ;

flight      : FLIGHT id=ID
              TYPE flightType
              ROUTE routeId=ID
              LBRACE leg+ RBRACE ;

flightType  : REGULAR | CHARTER ;

leg         : LEG LBRACE
                  departure
                  arrival
                  segment+
                  fuel
              RBRACE ;

departure   : DEPARTURE airportCode DATE TIME ;
arrival     : ARRIVAL   airportCode DATE TIME ;

segment     : SEGMENT LBRACE
                  START startC=coords
                  END   endC=coords
                  altitudeSlot+
                  WIND windAngle=number unitAngle COMMA windSpeed=number unitSpeed
              RBRACE ;

altitudeSlot: ALTITUDE alt=number unitLength
              WIDTH    wid=number unitLength ;

coords      : lat=number COMMA lon=number ;

fuel        : FUEL number unitMass ;

airportCode : AIRPORT_CODE ;

number      : MINUS? NUMBER ;

unitLength  : UNIT_M | UNIT_FT ;
unitSpeed   : UNIT_MS | UNIT_KT ;
unitMass    : UNIT_KG | UNIT_L ;
unitAngle   : UNIT_DEG ;



FLIGHT    : [fF][lL][iI][gG][hH][tT] ;
LEG       : [lL][eE][gG] ;
DEPARTURE : [dD][eE][pP][aA][rR][tT][uU][rR][eE] ;
ARRIVAL   : [aA][rR][rR][iI][vV][aA][lL] ;
ROUTE     : [rR][oO][uU][tT][eE] ;
SEGMENT   : [sS][eE][gG][mM][eE][nN][tT] ;
FUEL      : [fF][uU][eE][lL] ;
TYPE      : [tT][yY][pP][eE] ;
REGULAR   : [rR][eE][gG][uU][lL][aA][rR] ;
CHARTER   : [cC][hH][aA][rR][tT][eE][rR] ;
START     : [sS][tT][aA][rR][tT] ;
END       : [eE][nN][dD] ;
ALTITUDE  : [aA][lL][tT][iI][tT][uU][dD][eE] ;
WIDTH     : [wW][iI][dD][tT][hH] ;
WIND      : [wW][iI][nN][dD] ;



UNIT_MS   : [mM] '/' [sS] ;
UNIT_KT   : [kK][tT] ;
UNIT_KG   : [kK][gG] ;
UNIT_FT   : [fF][tT] ;
UNIT_DEG  : [dD][eE][gG] ;
UNIT_M    : [mM] ;
UNIT_L    : [lL] ;



LBRACE    : '{' ;
RBRACE    : '}' ;
COMMA     : ',' ;
MINUS     : '-' ;



DATE      : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT ;
TIME      : DIGIT DIGIT ':' DIGIT DIGIT ;
NUMBER    : DIGIT+ ('.' DIGIT+)? ;


AIRPORT_CODE : [A-Z][A-Z][A-Z][A-Z]?  ;

ID        : LETTER (LETTER | DIGIT | '_')* ;

fragment DIGIT  : [0-9] ;
fragment LETTER : [a-zA-Z_] ;



LINE_COMMENT  : '//' ~[\r\n]*    -> skip ;
BLOCK_COMMENT : '/*' .*? '*/'    -> skip ;
WS            : [ \t\r\n]+       -> skip ;