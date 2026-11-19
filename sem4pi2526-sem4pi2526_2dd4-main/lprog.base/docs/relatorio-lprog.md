# Relatório de Engenharia de Linguagem — Processador `FlightDSL`

**Auditoria técnica e funcional de um processador de linguagem em ANTLR4**

| | |
|---|---|
| **Artefacto auditado** | `lprog.base` |
| **Gramática** | `FlightDSL.g4` (combinada — léxico + sintaxe no mesmo ficheiro) |
| **Runtime** | ANTLR4 (Java target) |
| **Padrão de percurso** | Visitor (`FlightPlanBuilder`) + Listener (`Listener`) |
| **Suite de testes** | 40 ficheiros `.dfdsl` em `lprog.base/test/` |
| **Pipeline** | `Lexer → Parser → (Listener) → Visitor/Builder → SemanticValidator` (`FlightPlanImporter`) |

A `FlightDSL` é uma *Domain-Specific Language* para descrever **planos de voo**: um voo, do tipo `regular` ou `charter`, associado a uma rota e composto por uma ou mais *legs* (etapas). Cada *leg* tem partida, chegada, um ou mais *segments* com coordenadas, faixas de altitude e vento, e o respetivo combustível.

---

## 1. Nível Léxico (Scanning & Screening)

O analisador léxico (`FlightDSLLexer`, gerado a partir das regras em MAIÚSCULAS) agrupa a sequência de caracteres de entrada em **tokens**. Cada token é a categoria gramatical (a *unidade elementar*); o **lexema** é a sequência concreta de caracteres reconhecida.

### 1.1. Tokens, lexemas e expressões regulares

#### a) Palavras-chave (keywords)

Todas as palavras reservadas são definidas com classes de caracteres *case-insensitive*, o que torna a linguagem insensível a maiúsculas/minúsculas:

```
FLIGHT : [fF][lL][iI][gG][hH][tT] ;
LEG    : [lL][eE][gG] ;
WIND   : [wW][iI][nN][dD] ;
...
```

| Token | Lexema(s) aceites (exemplos) | ER (informal) |
|---|---|---|
| `FLIGHT` | `flight`, `FLIGHT`, `Flight` | `(f|F)(l|L)(i|I)(g|G)(h|H)(t|T)` |
| `REGULAR` / `CHARTER` | `regular` / `charter` | idem, *case-insensitive* |
| `START`, `END`, `ALTITUDE`, `WIDTH`, `WIND`, `FUEL`, `TYPE`, `ROUTE`, `LEG`, `SEGMENT`, `DEPARTURE`, `ARRIVAL` | as respetivas palavras | concatenação de classes `[xX]` |

Cada classe `[fF]` é, formalmente, a expressão regular *união* `(f|F)`. A concatenação destas classes define a palavra completa.

#### b) Unidades de medida

```
UNIT_MS  : [mM] '/' [sS] ;   // m/s
UNIT_KT  : [kK][tT] ;        // kt
UNIT_KG  : [kK][gG] ;        // kg
UNIT_FT  : [fF][tT] ;        // ft
UNIT_DEG : [dD][eE][gG] ;    // deg
UNIT_M   : [mM] ;            // m
UNIT_L   : [lL] ;            // L
```

#### c) Símbolos e pontuação

```
LBRACE : '{' ;  RBRACE : '}' ;  COMMA : ',' ;  MINUS : '-' ;
```

#### d) Tokens estruturados (com sub-padrões via *fragments*)

```
fragment DIGIT  : [0-9] ;
fragment LETTER : [a-zA-Z_] ;

DATE   : DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT ;  // YYYY-MM-DD
TIME   : DIGIT DIGIT ':' DIGIT DIGIT ;                              // HH:MM
NUMBER : DIGIT+ ('.' DIGIT+)? ;                                     // inteiro ou decimal
AIRPORT_CODE : [A-Z][A-Z][A-Z][A-Z]? ;                              // IATA(3) ou ICAO(4)
ID     : LETTER (LETTER | DIGIT | '_')* ;
```

Os **fragments** (`DIGIT`, `LETTER`) não produzem tokens próprios; são apenas sub-expressões reutilizáveis — o equivalente a *macros* de expressões regulares.

#### e) Metacaracteres de fecho presentes na gramática

| Metacaractere | Significado | Onde ocorre |
|---|---|---|
| **`+` (Fecho Positivo)** — uma ou mais | `DIGIT+` em `NUMBER`; `WS : [ \t\r\n]+` | `NUMBER`, `WS` |
| **`*` (Fecho de Kleene)** — zero ou mais | `(LETTER \| DIGIT \| '_')*` em `ID`; `~[\r\n]*` no comentário de linha; `.*?` no de bloco | `ID`, `LINE_COMMENT`, `BLOCK_COMMENT` |
| **`?` (Opcional)** — zero ou um | `('.' DIGIT+)?` em `NUMBER`; o 4.º `[A-Z]?` em `AIRPORT_CODE` | `NUMBER`, `AIRPORT_CODE` |

> Nota: `MINUS?` aparece também na regra de *parser* `number` (secção 2), não no léxico — o sinal negativo é um token `MINUS` separado, deliberadamente, para permitir validação semântica posterior.

### 1.2. Resolução de conflitos — *Maximal Munch* e prioridade

O lexer ANTLR resolve ambiguidades por duas regras combinadas:

1. **Substring mais longa (*maximal munch*)** — escolhe sempre o token que consome o maior número de caracteres.
2. **Ordem de declaração** — em caso de *empate* no comprimento, vence a regra declarada primeiro.

Estes mecanismos são determinantes nesta gramática:

- **`AIRPORT_CODE` vs `ID`** — para `LIS` (3 letras maiúsculas) **ambas** as regras casam com 3 caracteres. O empate é desfeito pela **ordem**: `AIRPORT_CODE` está declarado **antes** de `ID`, logo `LIS` → `AIRPORT_CODE`. Já `AA101` casa com `ID` (5 chars) e não com `AIRPORT_CODE` (que pararia em `AA`, insuficiente), pelo que *maximal munch* dá `ID`.
  > ⚠️ **Observação de auditoria:** um identificador de voo composto **apenas** por 3–4 letras maiúsculas (ex.: `ABCD`) seria classificado como `AIRPORT_CODE`, não como `ID`, e provocaria erro sintático na regra `flight`. Todos os IDs da suite (`AA101`, `BB202`, …) contêm dígitos, contornando o problema, mas trata-se de uma ambiguidade latente do desenho léxico.
- **`UNIT_M` vs `AIRPORT_CODE`** — `m` isolado → `UNIT_M` (1 char); `MAD` → `AIRPORT_CODE` (3 chars vence por comprimento).
- **`UNIT_KT` vs `ID`** — `kt` casa com ambas (2 chars); `UNIT_KT` declarado primeiro → vence.
- **Tokens compostos** — `>=` não existe aqui, mas o princípio é o mesmo aplicado a `DATE`/`TIME`: `2026-05-01` é reconhecido como **um único** token `DATE` e não como `NUMBER MINUS NUMBER MINUS NUMBER`, porque a alternativa `DATE` consome mais caracteres de uma só vez (*maximal munch*).

### 1.3. Tratamento de ruído (`-> skip`)

Sequências sem significado sintático são reconhecidas e **descartadas** antes de chegarem ao parser:

```
LINE_COMMENT  : '//' ~[\r\n]*  -> skip ;
BLOCK_COMMENT : '/*' .*? '*/'  -> skip ;
WS            : [ \t\r\n]+      -> skip ;
```

- `WS` elimina espaços, tabs e mudanças de linha (formatação livre).
- `LINE_COMMENT` usa `~[\r\n]*` — *negação de classe* (qualquer caractere exceto fim de linha), fecho de Kleene.
- `BLOCK_COMMENT` usa `.*?` — fecho de Kleene **não-guloso** (*lazy*), essencial para parar no **primeiro** `*/` e não consumir até ao último. Validado pelo teste `03` (comentário de bloco multilinha + comentário inline ambos ignorados).

---

## 2. Nível Sintático (Parsing)

### 2.1. Classificação na Hierarquia de Chomsky

| Componente | Regras | Tipo de Chomsky | Justificação |
|---|---|---|---|
| **Léxico** (MAIÚSCULAS) | `DATE`, `NUMBER`, `ID`, `AIRPORT_CODE`, … | **Tipo 3 — Regular** | Reconhecíveis por autómato finito; apenas expressões regulares (união, concatenação, fecho). |
| **Sintaxe** (minúsculas) | `flightPlan`, `flight`, `leg`, `segment`, … | **Tipo 2 — Independente do Contexto (CFG)** | Estruturas aninhadas e balanceadas (`{ … }`), recursão indireta; exigem autómato de pilha (LL(\*) no ANTLR). |

A regra de topo (símbolo inicial) é:

```
flightPlan : flight EOF ;
```

A presença explícita de `EOF` garante que **todo** o input é consumido — não pode sobrar texto após o voo (relevante no teste `16`, ver §5).

### 2.2. Estrutura hierárquica das produções

```
flightPlan → flight EOF
flight     → FLIGHT ID TYPE flightType ROUTE ID LBRACE leg+ RBRACE
leg        → LEG LBRACE departure arrival segment+ fuel RBRACE
segment    → SEGMENT LBRACE START coords END coords altitudeSlot+ WIND number unitAngle COMMA number unitSpeed RBRACE
coords     → number COMMA number
number     → MINUS? NUMBER
```

O aninhamento das chavetas (`flight { leg { segment { … } } }`) é precisamente o que requer poder **context-free**: uma gramática regular não consegue garantir o emparelhamento correto de `{` e `}` a profundidade arbitrária.

### 2.3. Cardinalidades e operadores de repetição

- `leg+` — um voo tem **pelo menos uma** *leg* (rejeita voo vazio).
- `segment+` — uma *leg* tem **pelo menos um** *segment* (teste `30`).
- `altitudeSlot+` — um *segment* tem **pelo menos uma** faixa de altitude (teste `28` usa várias).

Estes `+` no parser são *closures* positivas EBNF que o ANTLR compila internamente em laços, não em recursão à esquerda.

### 2.4. Recursividade

A gramática é essencialmente **iterativa** (usa `+` em vez de recursão), evitando por completo **recursão à esquerda** — que o ANTLR4 só tolera em regras de expressão. A única "recursão" é a estrutural/indireta (`flight` contém `leg`, que contém `segment`), que **deriva sempre até terminais**, garantindo terminação. Não há regras do tipo `x : x ...`.

### 2.5. Ambiguidade, precedência e *dangling else*

- **Precedência de operadores:** *não aplicável* — a `FlightDSL` é declarativa e **não possui expressões aritméticas nem operadores binários**. O único "operador" é o `MINUS` unário em `number : MINUS? NUMBER`, sem qualquer conflito de precedência. (Em ANTLR4, quando existem expressões, a precedência seria dada pela **ordem das alternativas** na regra de expressão, com os operadores mais prioritários "mais fundos" na árvore.)
- ***Dangling else*:** *não aplicável* — não há `if/then/else` nem estruturas de controlo opcionais que gerem o conflito clássico de associação do `else`. A linguagem é livre de ambiguidade de associação por construção: cada bloco é delimitado por `{ }`, o que torna toda a estrutura **explicitamente parentizada** (a mesma técnica que se usa para *resolver* o *dangling else* — forçar delimitadores).

> Em suma: o desenho com blocos `{ }` obrigatórios e cardinalidades `+` produz uma gramática **não ambígua** e LL(\*)-amigável, sem necessidade de predicados sintáticos.

### 2.6. Tratamento de erros sintáticos

`FlightPlanImporter` **remove** o `ConsoleErrorListener` por omissão e instala um `SyntaxErrorListener` próprio:

```16:25:lprog.base/src/parsing/SyntaxErrorListener.java
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        errors.add(new SyntaxError(line, charPositionInLine, msg));
    }
```

Os erros são acumulados (linha, coluna, mensagem) e, se existirem, o pipeline **aborta antes da semântica** devolvendo `ImportResult.syntaxFailure(...)`. O mesmo listener é partilhado por lexer e parser, pelo que erros **léxicos** (token não reconhecido) e **sintáticos** caem no mesmo canal.

---

## 3. Nível Semântico (Contexto e Tipos)

A análise sensível ao contexto está separada da gramática, implementada como um **conjunto de regras** (*Strategy pattern*) coordenadas por `SemanticValidator`:

```19:39:lprog.base/src/semantic/SemanticValidator.java
public class SemanticValidator {
    private final List<SemanticRule> rules = List.of(
            new UniqueFlightIdRule(),
            new LegContinuityRule(),
            new LegTimeOrderRule(),
            new RouteCoherenceRule(),
            new NoAirportRevisitRule(),
            new SegmentEndpointsRule(),
            new PositiveNumericRule(),
            new CoordinatesRangeRule(),
            new AirportCodeFormatRule(),
            new ValidDateTimeRule()
    );

    public List<SemanticError> validate(FlightPlan plan) {
        List<SemanticError> all = new ArrayList<>();
        for (SemanticRule rule : rules) {
            all.addAll(rule.validate(plan));
        }
        return all;
    }
}
```

Cada regra implementa a interface `SemanticRule` e devolve `List<SemanticError>`. **Todas** as regras são executadas (não há *short-circuit*), pelo que um plano pode acumular vários erros semânticos.

### 3.1. Tabela de Símbolos e gestão de âmbito (*scope*)

A `FlightDSL` **não tem variáveis, declarações nem referências cruzadas resolvidas por nome**; logo, **não existe uma tabela de símbolos clássica** (mapa nome → tipo/endereço/scope). O contexto é validado, em vez disso, sobre a **AST de domínio** já construída. Há, no entanto, dois *substitutos* funcionais de uma tabela de símbolos:

1. **`NoAirportRevisitRule`** mantém um **conjunto de símbolos** (`Set<AirportCode>`) que faz o papel de tabela: regista cada aeroporto "visitado" e deteta **redeclaração**/revisita — exatamente a função de uma tabela de símbolos que impede *declarações múltiplas*.

```20:35:lprog.base/src/semantic/rules/NoAirportRevisitRule.java
        Set<AirportCode> visited = new HashSet<>();
        visited.add(legs.get(0).departureAirport());
        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            AirportCode arr = leg.arrivalAirport();
            if (!visited.add(arr)) {
                errors.add(new SemanticError(
                        "NoAirportRevisit", leg.line(),
                        "Airport " + arr + " is visited more than once in the flight"));
            }
        }
```

2. **`UniqueFlightIdRule`** está preparada para garantir unicidade de identificadores de voo (o caso típico "nome já declarado"), mas, como a gramática só admite **um** voo por ficheiro (`flightPlan : flight EOF`), é hoje um *no-op* que devolve `List.of()` — código "morto" mantido por extensibilidade (ver §5, teste `16`).

O **âmbito** é, portanto, implícito e único: o do voo. Não há *scopes* aninhados porque não há identificadores definidos pelo utilizador para resolver.

### 3.2. Definições Dirigidas pela Sintaxe (DDS)

O `FlightPlanBuilder` (Visitor) é uma DDS prática:

- **Atributos sintetizados** — cada `visitXxx` **calcula um valor a partir dos filhos e devolve-o ao pai** (o valor "sobe" na árvore). Ex.: `visitCoords` sintetiza um `Coordinates` a partir de dois `number`; `visitSegment` agrega `start`, `end`, `slots`, `wind`; `visitFlight` agrega as *legs* num `Flight`; `visitFlightPlan` devolve o `FlightPlan` final — o atributo sintetizado de topo.

```106:111:lprog.base/src/parsing/FlightPlanBuilder.java
    @Override
    public Coordinates visitCoords(FlightDSLParser.CoordsContext ctx) {
        double lat = parseNumber(ctx.lat);
        double lon = parseNumber(ctx.lon);
        return new Coordinates(lat, lon, ctx.getStart().getLine());
    }
```

- **Atributos herdados** — em sentido estrito não há atributos a "descer" do pai para os filhos. O análogo mais próximo é o **número de linha** (`ctx.getStart().getLine()`), que é *propagado para baixo*, dos nós de contexto para os objetos de domínio, servindo de **atributo de localização** para mensagens de erro (cada `AirportCode`, `Coordinates`, `Leg`, `Fuel`, `AltitudeSlot` carrega o seu `line`). É o veículo que permite às regras semânticas reportar a posição exata da falha.

### 3.3. Sistema de Tipos, coerção e erros de tipo

A `FlightDSL` é **fracamente tipada ao nível da linguagem** mas **fortemente validada ao nível do domínio**:

- **Coerção numérica (int → real):** o token `NUMBER` cobre inteiros e decimais (`DIGIT+ ('.' DIGIT+)?`). O Visitor **coage uniformemente para `double`** via `Double.parseDouble(ctx.getText())` em `parseNumber`. Assim, `35000` (inteiro) e `35.5` (real) são tratados no mesmo "tipo" real — uma coerção implícita análoga à promoção `int → real` de um compilador.

```120:122:lprog.base/src/parsing/FlightPlanBuilder.java
    private double parseNumber(FlightDSLParser.NumberContext ctx) {
        return Double.parseDouble(ctx.getText());
    }
```

- **Tipagem por unidades:** as unidades funcionam como um *mini sistema de tipos dimensional*. A gramática **impõe estaticamente** a categoria correta por posição: `altitudeSlot` exige `unitLength` (`m`/`ft`), `WIND … unitAngle COMMA … unitSpeed` exige ângulo (`deg`) e velocidade (`m/s`/`kt`), `fuel` exige `unitMass` (`kg`/`L`). Uma unidade do tipo errado (ex.: `km/h` no vento) é um **erro de tipo detetado sintaticamente** (teste `07`).
- **Conversão data/hora com deteção de erro:** `parseDate`/`parseTime` tentam `LocalDate.parse`/`LocalTime.parse`; em falha **capturam a exceção e devolvem `null`** (em vez de propagar). Esse `null` é o **marcador de erro de tipo/valor** que a `ValidDateTimeRule` posteriormente converte em `SemanticError`. É a forma como o processador distingue "forma correta" (aceite pelo lexer) de "valor de calendário válido" (testes `22`, `23`, `35`, `39`).

```124:130:lprog.base/src/parsing/FlightPlanBuilder.java
    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s, DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
```

### 3.4. L-value vs R-value

**Não aplicável** no sentido clássico: a `FlightDSL` é **declarativa e sem atribuições** (`=`). Não existe lado esquerdo/direito de atribuição a validar, logo não há verificação de que apenas expressões com *l-value* (endereço de memória) possam ser alvo de escrita.

O conceito *aparece* unicamente como **rótulos de elementos da árvore** na gramática (`id=ID`, `routeId=ID`, `startC=coords`, `windAngle=number`, …). Estes `nome=` são *labels* ANTLR que dão acesso nomeado ao subnó no Visitor (`ctx.id`, `ctx.startC`) — são, conceptualmente, *r-values* lidos, nunca destinos de atribuição na linguagem-alvo.

---

## 4. Implementação e Geração de Código

Esta não é uma linguagem com *geração de código* (não emite bytecode/assembly); o "back-end" é a **construção de um modelo de domínio validado** (`FlightPlan`) — um *importer*. A implementação usa **os dois** padrões de percurso do ANTLR, com papéis distintos:

### 4.1. Listener vs Visitor — escolha e justificação

| Padrão | Classe | Percurso | Papel neste projeto |
|---|---|---|---|
| **Listener** | `parsing.Listener` | **Automático** (`ParseTreeWalker`), via eventos `enter/exit` | Diagnóstico/*tracing*: imprime a árvore indentada (Flight → Leg → Segment). Efeito **lateral**, sem retorno. |
| **Visitor** | `parsing.FlightPlanBuilder` | **Manual e explícito** (`visit(child)`), com **valor de retorno** | Construção da AST de domínio. Escolhido porque há **agregação de resultados dos filhos** (atributos sintetizados) — para o que o Visitor é idiomático. |

```50:62:lprog.base/src/app/FlightPlanImporter.java
        if (errorListener.hasErrors()) {
            return ImportResult.syntaxFailure(errorListener.errors());
        }

        ParseTreeWalker.DEFAULT.walk(new Listener(), tree);

        FlightPlanBuilder builder = new FlightPlanBuilder();
        FlightPlan plan = builder.visitFlightPlan(tree);

        List<SemanticError> semanticErrors = semanticValidator.validate(plan);
        if (!semanticErrors.isEmpty()) {
            return ImportResult.semanticFailure(semanticErrors);
        }
        return ImportResult.ok(plan);
```

A decisão é correta: **Listener** (percurso automático, ideal para travessia uniforme/observação) para o *trace*; **Visitor** (controlo manual, ideal para cálculos que retornam valores) para a transformação árvore → objetos. O `FlightPlanBuilder extends FlightDSLBaseVisitor<Object>` e faz *cast* dos retornos — funcional, embora um tipo de retorno mais específico (ex.: uma hierarquia selada) eliminasse os *casts* não verificados.

### 4.2. Ações semânticas embebidas

**Auditoria:** a gramática `FlightDSL.g4` **não contém ações semânticas embebidas** `{ … }` nem atributos `returns/locals` nem predicados `{...}?`. Esta é uma **boa prática de engenharia**: a gramática permanece **puramente declarativa e agnóstica da linguagem-alvo**, e toda a lógica (construção e validação) reside em Java testável (`FlightPlanBuilder`, regras `semantic.*`). A alternativa — inserir `{ tabela.put(...); }` no meio das regras — acoplaria a gramática ao Java e dificultaria a reutilização/teste. A separação adotada (gramática → Visitor → regras) é superior para manutenção.

### 4.3. Arquitetura do pipeline e tratamento de resultados

`ImportResult` modela o resultado como **um de três estados mutuamente exclusivos** (`ok` / `syntaxFailure` / `semanticFailure`) com listas imutáveis (`List.copyOf`), e `isValid()` só é verdadeiro sem erros. A ordem **léxico+sintático antes de semântico** está corretamente imposta: não se tenta validar semântica de uma árvore mal formada.

### 4.4. Modelo de domínio (notas de auditoria)

- `Coordinates` e `AirportCode` implementam `equals`/`hashCode` por valor — pré-requisito para `SegmentEndpointsRule` (igualdade de coordenadas) e `NoAirportRevisitRule` (conjunto de aeroportos) funcionarem corretamente.
- `AirportCode.equals` compara **apenas o `code`** (ignora `line`) — correto, pois a linha é metadado de diagnóstico e não identidade.
- `Leg` usa `List.copyOf` (imutabilidade) e `Objects.requireNonNull` nos aeroportos (invariantes de domínio).

---

## 5. Validação por Testes (Aceitação e Rejeição)

### 5.1. Critério (analogia com Autómatos Finitos)

Uma frase é **aceite** se o "autómato" completo (lexer + parser + validador) consegue uma derivação que consome **todo** o input (`EOF`) e termina sem erros léxicos, sintáticos ou semânticos — o análogo a *terminar num estado final*. É **rejeitada** se:

- **(Léxico)** não há transição válida para um caractere — token não reconhecido (paragem em estado não-final do AF do lexer);
- **(Sintático)** a sequência de tokens não tem derivação na CFG (o autómato de pilha bloqueia);
- **(Semântico)** a árvore é válida mas viola uma invariante de contexto.

A *suite* segue a convenção de nomes `NN_<fase>_<descrição>.dfdsl`, declarando a fase **esperada**. A auditoria abaixo confirma o **veredito** (aceitar/rejeitar) e, quando relevante, assinala divergências entre a **fase rotulada** e a **fase em que o erro é realmente detetado**.

### 5.2. Casos válidos (devem ser ACEITES)

| # | Ficheiro | Resultado | Porquê passa |
|---|---|---|---|
| 01 | `valid_direct_simple` | ✅ Aceite | Voo direto canónico; todos os tokens e regras satisfeitos. |
| 02 | `valid_multileg_3legs` | ✅ Aceite | `leg+`; continuidade LIS→MAD→CDG→LHR e tempos crescentes. |
| 03 | `valid_charter_with_comments` | ✅ Aceite | Valida `-> skip`: comentário de bloco multilinha e inline ignorados; unidades `ft`/`kt`/`L`. |
| 04 | `valid_negative_coordinates` | ✅ Aceite | `number : MINUS? NUMBER` permite latitudes/longitudes negativas (hemisfério sul/oeste), dentro de `[-90,90]`/`[-180,180]`. |
| 05 | `valid_multiple_segments` | ✅ Aceite | `segment+` numa mesma *leg*. |
| 26 | `valid_icao_airport_codes` | ✅ Aceite | `AIRPORT_CODE` com 4 letras (`LPPT`, `LEMD`); passa `AirportCodeFormatRule` (ICAO). |
| 27 | `valid_ft_and_kt_units` | ✅ Aceite | Unidades `ft` (comprimento) e `kt` (velocidade) corretamente tipadas. |
| 28 | `valid_multiple_altitude_slots` | ✅ Aceite | `altitudeSlot+` com 3 faixas. |
| 36 | `valid_multileg_four_legs` | ✅ Aceite | 4 *legs* encadeadas; continuidade, ordem temporal e coerência de rota satisfeitas. |

### 5.3. Casos inválidos (devem ser REJEITADOS)

Legenda da coluna *Fase real*: **LEX** = léxico · **SIN** = sintático · **SEM** = semântico.

| # | Ficheiro (rótulo) | Veredito | Fase real | Justificação técnica |
|---|---|---|---|---|
| 06 | `lexical_id_starts_with_number` | ✅ Rejeitado | **SIN** ⚠️ | `123AB` tokeniza como `NUMBER`(123)+`ID`(AB). O lexer **não** falha; o parser falha porque `flight` espera `ID` e recebe `NUMBER`. *Rotulado léxico, detetado no sintático.* |
| 07 | `lexicalinvalid_unit_kmh` | ✅ Rejeitado | **LEX+SIN** | `kmh`→`ID` (unidade inválida, falha sintática em `unitLength`); `km/h`: o `/` isolado **não tem token** → erro **léxico**. |
| 08 | `lexical_missing_unit` | ✅ Rejeitado | **SIN** ⚠️ | `altitude 35000 width…` e `fuel 3000` sem unidade: faltam `unitLength`/`unitMass`. Falha sintática (não léxica). |
| 09 | `lexical_keyword_typo` | ✅ Rejeitado | **SIN** ⚠️ | `flite`/`depart`/`segmant`/`fule` não casam as keywords → viram `ID`/`AIRPORT_CODE`; parser não encontra `FLIGHT`/`DEPARTURE`/… |
| 10 | `lexical_invalid_character` | ✅ Rejeitado | **LEX** ✓ | `@@@` não tem regra de token → erro de reconhecimento léxico. *Rótulo correto.* |
| 11 | `syntax_missing_fuel` | ✅ Rejeitado | **SIN** ✓ | `leg` exige `fuel` antes de `}`; ausente. |
| 12 | `syntax_missing_arrival` | ✅ Rejeitado | **SIN** ✓ | `leg` exige `arrival` após `departure`. |
| 13 | `syntax_missing_lbrace_flight` | ✅ Rejeitado | **SIN** ✓ | Falta `{` após `route TP012`. |
| 14 | `syntax_invalid_date_format` | ✅ Rejeitado | **SIN** ✓ | `01-05-2026` não casa o token `DATE` (espera `YYYY-MM-DD`); parser recebe `NUMBER` onde esperava `DATE`. |
| 15 | `syntax_missing_comma_coords` | ✅ Rejeitado | **SIN** ✓ | `coords : number COMMA number`; faltando a vírgula, falha. |
| 16 | `semantic_duplicate_flight_id` | ✅ Rejeitado | **SIN** ⚠️ | **Dois** voos no ficheiro. `flightPlan : flight EOF` só admite **um**; após o 1.º `}` o parser espera `EOF` e encontra `flight` → erro sintático. A `UniqueFlightIdRule` **nunca chega a correr** (e é *no-op*). *Rejeitado, mas no sintático, não no semântico.* |
| 17 | `semantic_fuel_negative` | ✅ Rejeitado | **SEM** ✓ | `fuel -100`: `PositiveNumericRule` (quantidade ≤ 0). |
| 18 | `semantic_arrival_departure_mismatch` | ✅ Rejeitado | **SEM** ✓ | `arr(leg1)=MAD ≠ dep(leg2)=CDG`: `LegContinuityRule`. |
| 19 | `semantic_time_incoherence` | ✅ Rejeitado | **SEM** ✓ | `dep(leg2)=09:30` anterior a `arr(leg1)=10:00`: `LegTimeOrderRule` (regra 2). |
| 20 | `semantic_airport_cycle` | ✅ Rejeitado | **SEM** ✓ | LIS→MAD→LIS: `NoAirportRevisitRule` (LIS revisitado). |
| 21 | `semantic_altitude_negative` | ✅ Rejeitado | **SEM** ✓ | `altitude -500`: `PositiveNumericRule` (altitude ≤ 0). |
| 22 | `semantic_date_feb_30` | ✅ Rejeitado | **SEM** ✓ | `2026-02-30` passa o token `DATE` (forma) mas `LocalDate.parse`→`null`→`ValidDateTimeRule`. |
| 23 | `semantic_time_25_00` | ✅ Rejeitado | **SEM** ✓ | `25:00` passa o token `TIME` (forma `DD:DD`) mas `LocalTime.parse`→`null`→`ValidDateTimeRule`. |
| 24 | `semantic_route_origin_mismatch` | ✅ Rejeitado | **SEM** ✓ | Rota `JFK001` (origem JFK) vs 1.ª partida `LIS`: `RouteCoherenceRule`. |
| 25 | `semantic_wind_speed_negative` | ✅ Rejeitado | **SEM** ✓ | `wind 270 deg, -20 m/s`: `PositiveNumericRule` (velocidade do vento < 0). |
| 29 | `syntax_missing_departure` | ✅ Rejeitado | **SIN** ✓ | `leg` exige `departure` antes de `arrival`. |
| 30 | `syntax_missing_segment` | ✅ Rejeitado | **SIN** ✓ | `segment+` exige ≥ 1 segmento; nenhum presente. |
| 31 | `syntax_segment_missing_wind` | ✅ Rejeitado | **SIN** ✓ | `segment` exige a cláusula `WIND …` antes de `}`. |
| 32 | `semantic_width_negative` | ✅ Rejeitado | **SEM** ✓ | `width -5`: `PositiveNumericRule` (largura ≤ 0). |
| 33 | `semantic_segment_same_coords` | ✅ Rejeitado | **SEM** ✓ | `start == end`: `SegmentEndpointsRule` (usa `Coordinates.equals`). |
| 34 | `semantic_fuel_zero` | ✅ Rejeitado | **SEM** ✓ | `fuel 0`: `PositiveNumericRule` exige **estritamente** positivo (≤ 0 falha). |
| 35 | `semantic_invalid_month_13` | ✅ Rejeitado | **SEM** ✓ | `2026-13-01` forma válida, mês inválido → `null` → `ValidDateTimeRule`. |
| 37 | `lexical_comma_as_decimal_separator` | ✅ Rejeitado | **SIN** ⚠️ | `38,7742`: o lexer produz `38`,`COMMA`,`7742`. `coords : number COMMA number` consome `38 , 7742` (lon=7742!); a `COMMA` seguinte deixa o parser a esperar `end` → erro **sintático**. *Rotulado léxico, detetado no sintático* (a vírgula é um token válido, só está fora de sítio). |
| 38 | `syntax_missing_route_keyword` | ✅ Rejeitado | **SIN** ✓ | `type regular TP038` sem `route`: parser espera `ROUTE`, recebe `ID`. |
| 39 | `semantic_date_day_zero` | ✅ Rejeitado | **SEM** ✓ | `2026-05-00` forma válida, dia inválido → `null` → `ValidDateTimeRule`. |
| 40 | `semantic_altitude_negative_ft` | ✅ Rejeitado | **SEM** ✓ | `altitude -1000 ft`: `PositiveNumericRule` (altitude ≤ 0), independentemente da unidade. |

### 5.4. Síntese dos testes

- **Veredito (aceitar vs rejeitar): 40/40 corretos.** Os 9 casos válidos são aceites; os 31 inválidos são rejeitados.
- **Cobertura por fase:** léxico (10), sintático (11, 12, 13, 14, 15, 29, 30, 31, 38), semântico (17–25, 32–35, 39, 40). Boa cobertura das três fases e de todas as 8 regras semânticas *ativas*.
- **Divergências rótulo↔fase real (5 casos):** `06`, `08`, `09`, `37` são rotulados *léxico/semântico* mas são **detetados no parser**; `16` é rotulado *semântico* mas é **detetado no parser** (gramática de voo único). São divergências **conceptualmente esperadas**: um erro "léxico" do ponto de vista do utilizador (typo, separador errado) traduz-se, num pipeline com tokens individualmente válidos, numa **falha sintática**. O sistema **rejeita corretamente** em todos eles; apenas a *classificação pedagógica* do nome do ficheiro não coincide com a camada técnica.

### 5.5. Observações e recomendações de auditoria

1. **`UniqueFlightIdRule` é código morto** dado `flightPlan : flight EOF`. Para tornar o teste `16` um verdadeiro caso *semântico* (e a regra útil), a gramática teria de aceitar múltiplos voos: `flightPlan : flight+ EOF ;`, passando a deteção de IDs duplicados para a regra semântica.
2. **Ambiguidade latente `AIRPORT_CODE` vs `ID`** (§1.2): um ID de voo só com letras maiúsculas seria mal classificado. Mitigável tornando `ID` prioritário onde aplicável ou exigindo formato distinto para IDs de voo.
3. **Classificação dos testes léxicos:** vários "léxicos" são na prática sintáticos. Sugere-se renomear (`06`, `08`, `09`, `37`) para `syntax_*`, ou aceitar a convenção atual como "fase percebida pelo utilizador".
4. **Pontos fortes:** separação limpa gramática/Visitor/regras (sem ações embebidas), imutabilidade do domínio, propagação de `line` para diagnóstico preciso, e a decisão acertada de validar **datas/horas** em duas etapas (forma no lexer, valor em `java.time`+regra) — um exemplo modelar de divisão de responsabilidades entre níveis léxico e semântico.

---

## 6. Conclusão

O processador `FlightDSL` é um *language front-end* **bem estruturado e correto** nos três níveis:

- **Léxico** — expressões regulares claras, *case-insensitivity* sistemática, *maximal munch* e prioridade por ordem bem explorados, e *screening* de ruído via `-> skip`.
- **Sintático** — CFG (Tipo 2) não ambígua, com blocos `{ }` que dispensam tratamento de *dangling else* e cardinalidades `+` em vez de recursão à esquerda; erros canalizados por um `BaseErrorListener` dedicado.
- **Semântico** — validação sensível ao contexto desacoplada da gramática (10 regras, 8 ativas), DDS por atributos sintetizados no Visitor, coerção numérica uniforme para `double`, tipagem dimensional por unidades e verificação de calendário delegada a `java.time`.

A *suite* de 40 testes confirma o comportamento esperado em **todos** os casos quanto a aceitação/rejeição. As únicas notas relevantes são (i) a regra de unicidade hoje inerte e (ii) algumas etiquetas de teste que nomeiam a fase "percebida" e não a fase técnica de deteção — nenhuma das quais compromete a correção do processador.
