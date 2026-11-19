# FlightDSL — Documentação Técnica

> Linguagem de Domínio Específico para Descrição de Planos de Voo  
> Processador gerado com **ANTLR4** · Linguagem de implementação: **Java**

---

## Índice

1. [Introdução e Descrição Informal](#1-introdução-e-descrição-informal)
2. [Análise Léxica (Tokens)](#2-análise-léxica-tokens)
3. [Análise Sintática (Gramática)](#3-análise-sintática-gramática)
4. [Análise Semântica e Processamento](#4-análise-semântica-e-processamento)
5. [Guia de Implementação (ANTLR4)](#5-guia-de-implementação-antlr4)
6. [Exemplo de Ficheiro Válido](#6-exemplo-de-ficheiro-válido)

---

## 1. Introdução e Descrição Informal

**FlightDSL** é uma linguagem textual de domínio específico (*Domain-Specific Language*) concebida para descrever **planos de voo** de forma formal, estruturada e passível de validação automática.

A regra inicial da gramática é `flightPlan`, que corresponde a um único bloco `flight` seguido do marcador de fim de ficheiro (`EOF`). Isto significa que **cada ficheiro FlightDSL descreve exactamente um voo**, identificado por um designador único, com um tipo (regular ou charter) e uma rota, subdividida em uma ou mais **legs** (etapas de voo).

Cada leg encapsula toda a informação operacional de um segmento não-stop: aeroporto e hora de partida, aeroporto e hora de chegada, um ou mais **segmentos de rota** (com coordenadas geográficas, slots de altitude, largura e dados de vento) e a quantidade de **combustível** carregado.

A linguagem destina-se a ser gerada por ferramentas externas especializadas e importada para o sistema AISafe, onde é sujeita a validação léxica, sintática e semântica antes de qualquer utilização em simulação.

---

## 2. Análise Léxica (Tokens)

### 2.1 Visão Geral dos Tokens

| Categoria | Tokens / Exemplos | Descrição |
|---|---|---|
| **Palavras-chave** | `FLIGHT`, `LEG`, `DEPARTURE`, `ARRIVAL`, `ROUTE`, `SEGMENT`, `FUEL`, `TYPE`, `REGULAR`, `CHARTER`, `START`, `END`, `ALTITUDE`, `WIDTH`, `WIND` | Estruturam o bloco hierárquico da linguagem |
| **Unidades** | `UNIT_MS`, `UNIT_KT`, `UNIT_KG`, `UNIT_FT`, `UNIT_DEG`, `UNIT_M`, `UNIT_L` | Qualificam valores numéricos |
| **Separadores** | `LBRACE` (`{`), `RBRACE` (`}`), `COMMA` (`,`) | Delimitam blocos e separam valores |
| **Literais** | `NUMBER`, `DATE`, `TIME`, `AIRPORT_CODE`, `ID` | Representam dados concretos |
| **Sinal** | `MINUS` (`-`) | Parte opcional de valores numéricos negativos |

### 2.2 Expressões Regulares dos Tokens Principais

#### Identificadores (`ID`)
```antlr
ID : LETTER (LETTER | DIGIT | '_')* ;
fragment LETTER : [a-zA-Z_] ;
fragment DIGIT  : [0-9] ;
```
Um identificador começa obrigatoriamente por uma letra (ou `_`) e pode ser seguido de qualquer combinação de letras, dígitos ou `_`. Utilizado para o identificador do voo (`id`) e da rota (`routeId`).

#### Códigos de Aeroporto (`AIRPORT_CODE`)
```antlr
AIRPORT_CODE : [A-Z][A-Z][A-Z][A-Z]? ;
```
Aceita apenas **maiúsculas**. Corresponde a:
- Códigos **IATA** (3 letras, ex.: `LIS`, `OPO`)
- Códigos **ICAO** (4 letras, ex.: `LPPT`, `LPPR`)

> ⚠️ Distinção face a `ID`: `AIRPORT_CODE` é exclusivamente maiúsculas e tem comprimento fixo (3–4). O ANTLR aplica a regra mais específica quando ambas poderiam corresponder; dado que `AIRPORT_CODE` restringe o alfabeto a `[A-Z]`, não há ambiguidade com `ID` que aceita minúsculas.

#### Números (`NUMBER`)
```antlr
NUMBER : DIGIT+ ('.' DIGIT+)? ;
```
Aceita inteiros (`42`) e valores de vírgula flutuante (`3.14`). O sinal negativo é tratado na regra sintática `number` (via `MINUS?`), separando a responsabilidade léxica da sintática.

#### Data (`DATE`) e Hora (`TIME`)
```antlr
DATE : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT ;
TIME : DIGIT DIGIT ':' DIGIT DIGIT ;
```
O formato de data é **ISO 8601** (`AAAA-MM-DD`) e a hora é `HH:MM`. A validação de valores calendários (ex.: mês 13, hora 25:00) é da responsabilidade da **análise semântica**.

#### Palavras-chave (case-insensitive)
```antlr
FLIGHT : [fF][lL][iI][gG][hH][tT] ;
```
Todas as palavras-chave são definidas com alternâncias de maiúsculas/minúsculas em cada posição, tornando a linguagem **insensível a maiúsculas** nas palavras reservadas. Identificadores e literais continuam a ser **sensíveis** a maiúsculas/minúsculas.

### 2.3 Tratamento de Elementos sem Significado Sintático

```antlr
LINE_COMMENT  : '//' ~[\r\n]*  -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
WS            : [ \t\r\n]+    -> skip ;
```

A diretiva `-> skip` instrui o ANTLR a **descartar** estes tokens antes de os enviar ao parser. Consequentemente:
- **Comentários de linha** (`// ...`) e **comentários de bloco** (`/* ... */`) são ignorados pelo parser mas podem ser preservados num canal oculto (`-> channel(HIDDEN)`) se for necessário manter metadados para ferramentas de IDE.
- **Espaço em branco** (espaços, tabs, quebras de linha) tem significado apenas de separação visual; não influencia a estrutura sintática.

---

## 3. Análise Sintática (Gramática)

### 3.1 Classificação da Gramática

A gramática `FlightDSL` é uma **Gramática Independente do Contexto (GIC) — Tipo 2** na hierarquia de Chomsky. As suas produções têm a forma `A → α`, onde `A` é um não-terminal e `α` é uma sequência (possivelmente vazia) de terminais e não-terminais.

O ANTLR4 gera um parser **LL(*)**, que usa análise preditiva descendente com retrocesso adaptativo, adequado para gramáticas sem ambiguidades estruturais como esta.

### 3.2 Hierarquia e Estrutura das Produções

A gramática tem uma estrutura **hierárquica e aninhada** sem operadores aritméticos, pelo que não existe necessidade de definir precedências de operadores. A estrutura é a seguinte:

```
flightPlan
└── flight
    ├── ID              (identificador do voo)
    ├── flightType      (REGULAR | CHARTER)
    ├── ID              (identificador da rota)
    └── leg+            (uma ou mais etapas)
        ├── departure   (aeroporto + data + hora)
        ├── arrival     (aeroporto + data + hora)
        ├── segment+    (um ou mais segmentos)
        │   ├── coords  (lat, lon de início)
        │   ├── coords  (lat, lon de fim)
        │   ├── altitudeSlot+ (altitude + largura)
        │   └── WIND    (ângulo + velocidade)
        └── fuel        (quantidade + unidade)
```

### 3.3 Produções Detalhadas

#### Plano de Voo e Voo
```antlr
flightPlan : flight EOF ;

flight : FLIGHT id=ID
         TYPE flightType
         ROUTE routeId=ID
         LBRACE leg+ RBRACE ;
```
O uso de **rótulos** (`id=`, `routeId=`) gera campos nomeados nas classes de contexto do ANTLR, facilitando o acesso direto na fase de processamento da árvore.

#### Etapa de Voo (Leg)
```antlr
leg : LEG LBRACE
          departure
          arrival
          segment+
          fuel
      RBRACE ;
```
A **ordem das sub-regras é fixa**: `departure`, `arrival`, `segment+` e `fuel` devem aparecer exactamente nesta sequência. Não existe ambiguidade de `dangling-else` pois não há estruturas condicionais.

#### Segmento
```antlr
segment : SEGMENT LBRACE
              START startC=coords
              END   endC=coords
              altitudeSlot+
              WIND windAngle=number unitAngle COMMA windSpeed=number unitSpeed
          RBRACE ;
```

#### Valores Numéricos com Sinal
```antlr
number : MINUS? NUMBER ;
```
O `MINUS` opcional permite representar coordenadas negativas (ex.: longitude negativa para o hemisfério ocidental) e outros valores negativos. Esta abordagem evita a criação de um token `SIGNED_NUMBER` no léxico, separando a responsabilidade de forma mais limpa.

#### Unidades
```antlr
unitLength : UNIT_M | UNIT_FT ;
unitSpeed  : UNIT_MS | UNIT_KT ;
unitMass   : UNIT_KG | UNIT_L ;
unitAngle  : UNIT_DEG ;
```
As unidades são **fortemente tipadas** na gramática: cada contexto aceita apenas as unidades semanticamente compatíveis. Isto previne, por exemplo, especificar altitude em `kg`.

### 3.4 Operador de Repetição

| Símbolo | Significado | Uso na gramática |
|---|---|---|
| `+` | Um ou mais | `leg+`, `segment+`, `altitudeSlot+` |
| `?` | Zero ou um | `MINUS?` em `number` |

Não existem repetições do tipo `*` (zero ou mais), o que reflecte que todos os blocos são obrigatórios.

---

## 4. Análise Semântica e Processamento

### 4.1 Sistema de Tipos

A FlightDSL não é uma linguagem de programação de uso geral, pelo que não possui um sistema de tipos estático explícito. Contudo, a gramática impõe **restrições de tipo implícitas** através das regras de unidades:

| Tipo semântico | Regra sintática | Unidades aceites |
|---|---|---|
| Distância / Altitude | `unitLength` | `m`, `ft` |
| Velocidade | `unitSpeed` | `m/s`, `kt` |
| Massa / Volume de combustível | `unitMass` | `kg`, `l` |
| Ângulo de vento | `unitAngle` | `deg` |
| Coordenada geográfica | `number` (em `coords`) | sem unidade (graus decimais implícitos) |

A validação de **compatibilidade de unidades** (ex.: mistura de `m` e `ft` no mesmo cálculo) é responsabilidade da análise semântica implementada no Visitor/Listener.

### 4.2 Atributos Sintetizados e Herdados

No paradigma de gramáticas com atributos (Attribute Grammars):

**Atributos sintetizados** (calculados a partir dos filhos, propagam-se para cima):
- O valor numérico final de `number` é sintetizado a partir de `MINUS?` e `NUMBER`.
- O código do aeroporto em `airportCode` é sintetizado a partir de `AIRPORT_CODE`.
- A representação de `coords` (par lat/lon) é sintetizada a partir de dois `number`.
- O identificador completo do voo e da rota são sintetizados a partir de `ID`.

**Atributos herdados** (propagam-se do pai para os filhos, úteis para verificação de contexto):
- O contexto da leg corrente (índice da leg) pode ser herdado pelos `segment` para validação da coerência sequencial.
- O aeroporto de chegada da leg `n` pode ser herdado para verificar se coincide com a partida da leg `n+1`.
- O tipo de voo (`REGULAR` | `CHARTER`) pode ser herdado para validar restrições específicas de cada tipo.

### 4.3 Regras de Validação Semântica

Após a validação léxica e sintática bem-sucedida, devem ser aplicadas as seguintes verificações:

#### Nível do Voo
- O identificador do voo (`id`) deve ser **único** no ficheiro.
- O `flightType` deve ser `REGULAR` ou `CHARTER` (garantido pela gramática).

#### Nível da Leg
- Cada leg deve ter **exactamente** uma `departure` e uma `arrival`.
- Cada leg deve ter **pelo menos** um `segment`.
- O valor de `fuel` deve ser **estritamente positivo**.

#### Coerência de Sequência de Legs
- O aeroporto de **chegada** da leg `n` deve coincidir com o aeroporto de **partida** da leg `n+1`.
- A hora de chegada da leg `n` deve ser **anterior** à hora de partida da leg `n+1`.

#### Coerência da Rota
- O aeroporto de origem da rota deve coincidir com a partida da **primeira** leg.
- O aeroporto de destino da rota deve coincidir com a chegada da **última** leg.
- O mesmo aeroporto **não pode ser visitado duas vezes** no mesmo voo.

#### Coerência dos Segmentos
- As coordenadas de início e fim de um `segment` devem ser **diferentes**.
- Cada `segment` pertence a **exactamente** uma leg.

#### Consistência de Domínio
- Códigos de aeroporto devem ter 3 (IATA) ou 4 (ICAO) caracteres maiúsculos — garantido pelo léxico.
- Valores numéricos de altitude, largura, velocidade de vento e combustível devem ser **positivos**.
- Datas e horas devem representar **valores calendários válidos** (ex.: mês ≤ 12, hora ≤ 23).

> Todos os erros semânticos devem ser **recolhidos e reportados numa única execução**, sem paragem no primeiro erro.

### 4.4 Tabela de Símbolos

Embora a FlightDSL não declare variáveis, é necessário manter uma estrutura equivalente a uma tabela de símbolos para:

| Entidade | Finalidade |
|---|---|
| Identificadores de voo (`ID`) | Detectar duplicados no mesmo ficheiro |
| Identificadores de rota (`routeId`) | Verificar referência válida |
| Códigos de aeroporto por leg | Verificar coerência entre legs consecutivas |
| Aeroportos visitados | Detectar visitas duplicadas |

Esta tabela é tipicamente construída durante a **primeira passagem** do Visitor/Listener e consultada nas validações semânticas da **segunda passagem**.

---

## 5. Guia de Implementação (ANTLR4)

### 5.1 Geração do Processador

```bash
# Gerar lexer, parser, listener e visitor
antlr4 -Dlanguage=Java -visitor -listener FlightDSL.g4

# Compilar
javac *.java

# Executar
grun FlightDSL flightPlan -tree input.flight
```

### 5.2 Rótulos (Labels) e Classes de Contexto

A gramática utiliza rótulos de atributos que o ANTLR traduz em campos tipados nas classes de contexto:

```antlr
flight : FLIGHT id=ID TYPE flightType ROUTE routeId=ID ... ;
```

Gera a classe `FlightDSLParser.FlightContext` com os campos:
```java
public Token id;      // ctx.id.getText()  → identificador do voo
public Token routeId; // ctx.routeId.getText() → identificador da rota
```

```antlr
segment : SEGMENT LBRACE
              START startC=coords
              END   endC=coords ... ;
```

Gera:
```java
public CoordsContext startC; // ctx.startC → contexto das coordenadas de início
public CoordsContext endC;   // ctx.endC   → contexto das coordenadas de fim
```

### 5.3 Uso de Listeners

O **Listener** é adequado para percursos simples onde a ordem de entrada/saída dos nós é suficiente (ex.: construção de um relatório, extração de dados para um DTO).

```java
public class FlightDSLListener extends FlightDSLBaseListener {

    @Override
    public void enterFlight(FlightDSLParser.FlightContext ctx) {
        String flightId = ctx.id.getText();
        String routeId  = ctx.routeId.getText();
        System.out.println("Voo: " + flightId + " | Rota: " + routeId);
    }

    @Override
    public void enterDeparture(FlightDSLParser.DepartureContext ctx) {
        String airport = ctx.airportCode().getText();
        String date    = ctx.DATE().getText();
        String time    = ctx.TIME().getText();
        System.out.println("  Partida: " + airport + " em " + date + " às " + time);
    }

    @Override
    public void enterFuel(FlightDSLParser.FuelContext ctx) {
        String qty  = ctx.number().getText();
        String unit = ctx.unitMass().getText();
        System.out.println("  Combustível: " + qty + " " + unit);
    }
}
```

**Quando usar Listener:** extração de dados, logging, geração de relatórios.

### 5.4 Uso de Visitors

O **Visitor** é adequado para quando é necessário **retornar valores** a partir dos nós da árvore (ex.: construção de objectos de domínio, avaliação de expressões, validação semântica com acumulação de erros).

```java
public class FlightDSLVisitor extends FlightDSLBaseVisitor<FlightPlan> {

    @Override
    public FlightPlan visitFlight(FlightDSLParser.FlightContext ctx) {
        String id      = ctx.id.getText();
        String routeId = ctx.routeId.getText();
        FlightType type = visitFlightType(ctx.flightType());

        List<Leg> legs = ctx.leg().stream()
                            .map(this::visitLeg)
                            .collect(Collectors.toList());

        return new FlightPlan(id, routeId, type, legs);
    }

    @Override
    public FlightPlan visitLeg(FlightDSLParser.LegContext ctx) {
        // Construção do objecto de domínio Leg
        Departure dep = visitDeparture(ctx.departure());
        Arrival   arr = visitArrival(ctx.arrival());
        Fuel      fuel = visitFuel(ctx.fuel());
        List<Segment> segs = ctx.segment().stream()
                                .map(this::visitSegment)
                                .collect(Collectors.toList());
        return new Leg(dep, arr, segs, fuel);
    }
}
```

**Quando usar Visitor:** construção de AST/objectos de domínio, validação semântica com retorno de valores, transformações da árvore.

### 5.5 Tratamento de Erros Léxicos e Sintáticos

O ANTLR4 fornece mecanismos de reporte de erros que devem ser personalizados para mensagens informativas:

```java
public class FlightDSLErrorListener extends BaseErrorListener {

    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line, int charPositionInLine,
                            String msg, RecognitionException e) {
        String error = String.format(
            "[ERRO SINTÁTICO] Linha %d, Coluna %d: %s",
            line, charPositionInLine, msg
        );
        errors.add(error);
    }

    public List<String> getErrors() { return errors; }
    public boolean hasErrors()      { return !errors.isEmpty(); }
}
```

**Integração:**
```java
FlightDSLLexer  lexer  = new FlightDSLLexer(CharStreams.fromFileName("input.flight"));
FlightDSLParser parser = new FlightDSLParser(new CommonTokenStream(lexer));

FlightDSLErrorListener errorListener = new FlightDSLErrorListener();
lexer.removeErrorListeners();
lexer.addErrorListener(errorListener);
parser.removeErrorListeners();
parser.addErrorListener(errorListener);

ParseTree tree = parser.flightPlan();

if (errorListener.hasErrors()) {
    errorListener.getErrors().forEach(System.err::println);
    System.exit(1);
}

// Percurso com Visitor
FlightPlan plan = new FlightDSLVisitor().visit(tree);
```

### 5.6 Recomendação de Arquitectura de Processamento

```
Ficheiro .flight
      │
      ▼
  FlightDSLLexer          ← Análise Léxica (tokens)
      │
      ▼
  FlightDSLParser          ← Análise Sintática (parse tree)
      │
      ▼
  FlightDSLVisitor         ← Construção da Representação Interna (AST / domain objects)
      │
      ▼
  SemanticValidator        ← Validação Semântica (regras de domínio)
      │
      ▼
  FlightPlan (domínio)     ← Objecto pronto para uso no sistema AISafe
```

> A gramática **não contém acções semânticas embebidas** (código entre `{ }`), o que é uma boa prática: mantém a gramática limpa e delega toda a lógica de processamento para os Visitors/Listeners em Java.

---

## 6. Exemplo de Ficheiro Válido

```
// Plano de voo de exemplo — Lisboa para Frankfurt com escala em Madrid
FLIGHT TP1234A
  TYPE regular
  ROUTE LPPT_EDDF
  {
    LEG {
      DEPARTURE LIS 2026-06-15 08:00
      ARRIVAL   MAD 2026-06-15 09:30

      SEGMENT {
        START 38.7749, -9.1341
        END   40.4168, -3.7038
        ALTITUDE 10000 m WIDTH 50 m
        ALTITUDE 11000 m WIDTH 50 m
        WIND 270 deg, 15 m/s
      }

      FUEL 12500 kg
    }

    LEG {
      DEPARTURE MAD 2026-06-15 11:00
      ARRIVAL   FRA 2026-06-15 14:15

      SEGMENT {
        START 40.4168, -3.7038
        END   50.0379, 8.5622
        ALTITUDE 11000 m WIDTH 60 m
        WIND 290 deg, 20 m/s
      }

      FUEL 18000 kg
    }
  }
```

---

## Referências

- ANTLR4 Reference — [https://github.com/antlr/antlr4/blob/master/doc/index.md](https://github.com/antlr/antlr4/blob/master/doc/index.md)
- *The Definitive ANTLR4 Reference* — Terence Parr, Pragmatic Bookshelf
- AISafe Project Requirements v2.a — ISEP, 2025/2026
- Hierarquia de Chomsky — Hopcroft, Motwani & Ullman, *Introduction to Automata Theory*