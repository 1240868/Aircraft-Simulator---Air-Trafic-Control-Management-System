# Engine model Done

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Baseada no Enunciado) | Razão Técnica do Value Object |
| :--- | :--- | :--- |
| **EngineModelId** | Identificador único global do modelo de motor. | **VO:** Garante a imutabilidade da identidade (UUID) para referências externas. |
| **EngineType** | "...turboprop, turbofan, turbojet, ramjet, electric propeller." [3.2] | **VO (Enum):** Define o comportamento do motor e dita qual a lógica de cálculo (térmica ou elétrica) a aplicar. |
| **FuelType** | "...fuel used by the engine (e.g. JET A-1, electricity)." [3.2] | **VO:** Encapsula a **densidade** (usada para converter volume em peso no avião) e a **energia** (usada para autonomia elétrica). |
| **jetEngineSpecs** | "...thrust (e.g. at sea level static and at cruise speed) and TSFC." [3.2] | **VO Opcional:** Agrupa parâmetros exclusivos de motores a jato. O **TSFC** é a chave para calcular a queima de massa de combustível líquido. |
| **ElectricEngineSpecs** | "...power and efficiency (only for electric propellers)." [3.2] | **VO Opcional:** Agrupa parâmetros exclusivos de motores elétricos, onde a **eficiência** substitui o conceito de queima de combustível. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. EngineModel (1) *-- (0..1) jetEngineSpecs / ElectricEngineSpecs**
* **Tipo:** Composição Opcional.
* **Justificação:** O enunciado apresenta uma **dualidade tecnológica**. Motores a jato não se definem por potência ($W$), e motores elétricos não têm tração estática de catálogo nem queima de massa (TSFC).
* **Razão:** A multiplicidade `0..1` permite que o motor seja **um ou outro**, impedindo estados inconsistentes (ex: um motor elétrico com parâmetros de jato).

#### **B. EngineModel (*) *-- (1) FuelType**
* **Tipo:** Composição.
* **Justificação:** Cada motor consome um tipo específico de combustível [3.2].
* **Razão:** Permite que vários modelos de motor (ex: todos os Turbofans) partilhem a mesma definição física de **JET A-1** (densidade 0.804 kg/l).

#### **C. EngineModel (1) --> (1) MakerId**
* **Tipo:** Referência por ID.
* **Justificação:** Cada modelo de motor é produzido por um fabricante específico.
* **Razão:** Mantém o agregado `EngineModel` focado na performance técnica, delegando os dados do fabricante para o agregado `AircraftMaker`.

---

### 3. Justificação dos Métodos de Domínio

| Método | Justificação Técnica e Física |
| :--- | :--- |
| **`calculateThrust`** | Essencial para a Secção 6.1 ($L=W$ e $T=D$). Se o motor for Jato, interpola a tração pela altitude. Se for Elétrico, converte Potência em Tração ($T = P \times \eta / V$). |
| **`calculateFuelConsumption`** | Implementa a US010 (Simulação). Para Jatos, calcula a perda de peso (massa) via TSFC. Para Elétricos, calcula a drenagem de energia (Joules) da bateria. |
| **`isCompatibleWith`** | Garante a regra de negócio: *"Engine type must be compatible with the aircraft model's motorization."* [3.2]. |

---

# Aircraft model Done 

## 1. Justificação dos Atributos e Value Objects (VO)

| Atributo / VO | Justificação (Transcrição do Enunciado) | Razão do Tipo (Value Object) |
| :--- | :--- | :--- |
| **AircraftModelId** | "Each aircraft model is uniquely identified by its name and manufacturer." [3.2] | **VO:** Garante a imutabilidade e encapsula a lógica de identidade (UUID). |
| **AircraftType** | "...type of aircraft (passenger, cargo, mixed)..." [3.2] | **VO (Enum):** Restringe os valores aos tipos permitidos pelo negócio. |
| **Weights (Empty, MTOW, MZFW)** | "...Maximum Take-Off Weight (MTOW) ... Maximum Zero Fuel Weight (MZFW) ... empty weight." [3.2] | **VO:** Garante que o valor e a unidade (kg/ton) viajam juntos, evitando erros de cálculo. |
| **MaxFuelCapacity** | "...maximum fuel capacity." [3.2] | **VO:** Necessário para validar o abastecimento na US010. |
| **ServiceCeiling / CruiseSpeed** | "...service ceiling ... cruise speed." [3.2] | **VO:** Define os limites operacionais para a validação de planos de voo. |
| **WingArea** | "...wing area ($A$)..." [6.1] | **VO:** Parâmetro essencial para o cálculo da sustentação ($L$). |
| **AerodynamicCoefficients** | "...Drag coefficient ($Cd$) ... Lift coefficient ($Cl$)." [3.2] | **VO:** Valores adimensionais que representam a eficiência aerodinâmica. |
| **AspectRatio ($AR$) / Oswald ($e$)** | "...$AR$ is the aspect ratio ... $e$ is the Oswald efficiency factor." [6.1] | **VO:** Parâmetros de design da asa usados na fórmula física do arrasto. |
| **NumberOfEngines** | "...number of engines..." [3.2] | **Atributo Primitivo:** Um inteiro simples necessário para calcular a tração total. |

---

## 2. Justificação das Ligações e Multiplicidades

### **A. AircraftModel (1) *-- (3) Weight**
* **Tipo:** Composição (Value Object).
* **Justificação:** O enunciado distingue três pesos críticos. A multiplicidade **3** garante que o modelo possui os três limites estruturais necessários para as fórmulas de física e segurança de voo.

### **B. AircraftModel (1) *-- (1..3) AircraftClass**
* **Tipo:** Composição (Value Object).
* **Justificação:** *"Registration must include... maximum number of seats for each class (e.g. first class, executive class, economy class)."* [3.2].
* **Razão:** Um modelo pode ter apenas uma classe (ex: Económica) ou as três. A ligação `1..3` reflete esta flexibilidade de configuração do fabricante.

### **C. AircraftModel (1) --> (0..*) EngineModelId**
* **Tipo:** Agregação por ID (Referência Externa).
* **Justificação:** *"The aircraft model must include the models of engines that are certified for it."* [3.2].
* **Razão:** Segue as boas práticas de DDD; não incluímos o objeto `EngineModel` inteiro para evitar um agregado gigante, apenas referenciamos os IDs dos motores permitidos.

### **D. AircraftModel (1) *-- (2) AerodynamicCoefficient**
* **Tipo:** Composição (Value Object).
* **Justificação:** O modelo requer dois coeficientes distintos ($Cl$ e $Cd$) para resolver as equações de sustentação e arrasto da Secção 6.1.

---

## 3. Justificação dos Métodos de Domínio

| Método | Justificação (Regra de Negócio / Física) |
| :--- | :--- |
| **`isEngineCompatible`** | *"Engine type must be compatible with the aircraft model's motorization."* [3.2] |
| **`addCertifiedEngine`** | Necessário para cumprir a US056: *"As a Fleet Manager, I want to add an engine model to the list of certified engines..."* |
| **`removeCertifiedEngine`** | *"An aircraft model must maintain at least one certified engine."* [3.2] - O método deve validar esta invariante antes de remover. |
| **`validateModelUniqueness`** | *"The model’s name and manufacturer combination must be unique."* [3.2] |
| **`calculateTotalDrag`** | Implementa a fórmula: $C_{D} = C_{D0} + \frac{C_{L}^{2}}{\pi ARe}$ [6.1]. Essencial para a simulação de consumo (US010). |

---

# Aircraft Done 

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Baseada no Enunciado) | Razão Técnica do Value Object |
| :--- | :--- | :--- |
| **RegistrationNumber** | "Registration (unique identification of the aircraft within the fleet)" [3.1] | **VO:** Garante que a matrícula segue o formato oficial (ex: CS-TNP). Como notaste, o prefixo já identifica o **país**, eliminando a necessidade de um campo `country` separado. |
| **CabinConfiguration** | "...actual number of seats for each class (first, executive, economy)..." [3.1] | **VO:** Agrupa as três classes num único objeto imutável. Garante que os lugares são tratados como uma "unidade de configuração" e não como números soltos. |
| **CrewSize** | "...number of cabin crew elements." [3.1] | **VO:** Garante a **invariante básica**: o número de tripulantes não pode ser negativo. Substituir o `Integer` por um VO protege o domínio de estados impossíveis. |
| **AircraftStatus** | Necessário para a regra: "An aircraft model cannot be removed if there are aircrafts using it." [3.2] | **VO (Enum):** Permite controlar o ciclo de vida. Um avião `DECOMMISSIONED` liberta o modelo para ser removido, enquanto um `OPERATIONAL` bloqueia a remoção. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. Aircraft (1) *-- (1) AircraftId**
* **Tipo:** Composição (Identidade).
* **Justificação:** Todo o agregado precisa de uma identidade única global (UUID) para ser referenciado por outros agregados como o **FlightPlan**.

#### **B. Aircraft (1) --> (1) CompanyId**
* **Tipo:** Referência por ID (Agregação Externa).
* **Justificação:** *"Registration must include the air transport company that owns the aircraft."* [3.1].
* **Razão:** Não incluímos o objeto `Company` inteiro para manter o agregado `Aircraft` leve e independente (desacoplamento DDD).

#### **C. Aircraft (1) --> (1) AircraftModelId**
* **Tipo:** Referência por ID.
* **Justificação:** *"Registration must include... the aircraft model."* [3.1].
* **Razão:** É a ligação vital para obter os limites de peso e capacidade durante as validações de voo.

#### **D. Aircraft (1) --> (1) EngineModelId**
* **Tipo:** Referência por ID.
* **Justificação:** *"Registration must include... the engine model that is used in that aircraft."* [3.1].
* **Razão:** Permite verificar se o motor instalado é um dos certificados pelo fabricante do modelo.

---

### 3. Justificação dos Métodos de Domínio

| Método | Justificação Técnica e de Negócio |
| :--- | :--- |
| **`validateCabinConfig`** | *"The actual number of seats... must be lower than or equal to the maximum number... defined for that aircraft model."* [3.1]. Este método faz a ponte entre a configuração real e os limites do modelo. |
| **`hasCertifiedEngine`** | Garante que o motor instalado na aeronave física consta na lista de motores permitidos pelo fabricante (`AircraftModel.certifiedEngineIds`). |
| **`decommission`** | Implementa a lógica de retirar o avião de serviço permanentemente. É o gatilho que permite que um modelo de avião possa vir a ser apagado do sistema no futuro. |
| **`isOperational`** | Método de consulta rápido usado pelo sistema de agendamento de voos para saber se este avião específico pode ser alocado a uma rota. |

---

# Aircraft maker Done 

Aqui tens a justificação técnica e normativa do agregado **AircraftMaker**, estruturada para o teu relatório ou defesa, focada na autoridade de design e na integridade do catálogo de modelos.

---

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Baseada no Enunciado) | Razão Técnica do Value Object |
| :--- | :--- | :--- |
| **MakerId** | Identificador único do fabricante. | **VO:** Garante a imutabilidade da identidade (UUID) para referências externas em `AircraftModel` e `EngineModel`. |
| **Name** | "...uniquely identified by its name and manufacturer." [3.2] | **Atributo:** Chave de negócio essencial. Sem um nome de fabricante validado, a identificação única de um modelo é impossível. |
| **OriginCountry** | Requisito implícito de certificação internacional (ICAO). | **VO:** Encapsula a nacionalidade do fabricante, crucial para normas de exportação e registo de aeronaves. |
| **makerType** | "...engine model... aircraft model..." [3.1/3.2] | **VO (Enum):** Define a **competência industrial** da entidade. Garante que um fabricante de motores não seja usado para registar um modelo de avião (e vice-versa). |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. AircraftMaker (1) *-- (1) MakerId / OriginCountry / makerType**
* **Tipo:** Composição (Value Objects).
* **Justificação:** Um fabricante não existe no sistema sem uma identidade, uma origem e um propósito definido (tipo de fabrico). Estes elementos formam o estado atómico do agregado.

#### **B. Referências de Entrada (Incoming References)**
* **Justificação:** O `MakerId` é referenciado pelo `EngineModel` e pelo `AircraftModel`.
* **Razão:** Segue a regra de **Referência por ID** do DDD. Isto permite que os modelos técnicos conheçam o seu "dono" (quem os projetou) sem carregar o objeto fabricante inteiro, mantendo o sistema leve e desacoplado.

---

### 3. Justificação dos Métodos de Domínio

| Método | Justificação Técnica e de Negócio |
| :--- | :--- |
| **`canProduceAircraft` / `canProduceEngine`** | Implementam as **Invariantes de Competência**. Impedem que o sistema associe um modelo de motor a um fabricante que apenas produz fuselagens (ex: Boeing não fabrica motores a jato, a Rolls-Royce sim). |
| **`isOwnerOf(Model)`** | Valida a **Propriedade Intelectual**. Garante que um modelo (Aviation ou Engine) está corretamente "assinado" pelo seu fabricante de origem durante operações de edição ou consulta. |
| **`validateMakerUniqueness`** | Garante que não existem duplicados no "catálogo de autoridades". Se existissem dois fabricantes com o mesmo nome, a regra de unicidade do modelo [3.2] colapsaria. |

---

# AirControl Area Done 

---

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Simulação/Negócio) | Razão Técnica (DDD) |
| :--- | :--- | :--- |
| **AirControlAreaId** | Identidade única global do setor. | **VO:** Permite que aeroportos e dados meteorológicos se ancorem a uma região geográfica específica. |
| **horizontalLimits** | Define a "fronteira" lateral (Norte, Sul, Este, Oeste). | **VO:** Agrupa as quatro coordenadas necessárias para formar um retângulo de controlo. Garante a **Invariante Espacial**: a área só existe se todos os limites estiverem definidos. |
| **VerticalLimits** | Define o "piso" e o "teto" da área (Altitude/FL). | **VO:** Fundamental para a simulação de trajetória. Determina se um avião está sob o controlo desta área ou a voar por cima/baixo dela. |
| **FuelQuantity** | "...actual number of fuel... must be higher than reserve." [Simulação] | **VO:** Define a reserva mínima de segurança exigida para transitar neste setor específico do céu. |
| **AirControlType** | Distingue entre **CTR** (Terminal) e **CTA** (Cruzeiro). | **VO (Enum):** Permite aplicar regras de separação e prioridades de tráfego diferentes consoante a natureza da área. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. AirControlArea (1) *-- (1) horizontalLimits / VerticalLimits**
* **Tipo:** Composição 1 para 1.
* **Justificação:** Ao contrário da versão anterior, agora tratas os limites como **objetos atómicos**. Um `horizontalLimits` é uma "caixa" única. Isto evita que o agregado fique num estado inconsistente (ex: ter um Norte mas não ter um Sul).

#### **B. AirControlArea (*) *-- (1) AirControlType**
* **Tipo:** Composição (Value Object partilhado).
* **Justificação:** Várias áreas podem partilhar o mesmo tipo (ex: existem centenas de CTRs no mundo), mas cada área tem o seu comportamento ditado por esse tipo.

#### **C. Referências de Entrada (Incoming)**
* **Airport.areaId:** Um aeroporto deve estar contido numa área de controlo (CTR) para que as descolagens e aterragens sejam validadas.
* **WeatherData.areaId:** Permite que a meteorologia (ventos, tempestades) afete apenas os aviões que estão dentro dos limites desta `AirControlArea`.

---

### 3. Justificação dos Métodos de Domínio

| Método | Justificação Técnica e de Negócio |
| :--- | :--- |
| **`isPointInside`** | É o método central da simulação. O sistema de voo usa-o para detetar o momento exato em que o avião entra ou sai da jurisdição desta área, aplicando as regras de reserva de combustível e as taxas de controlo associadas. |

---
# weather data Done 

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Baseada no Enunciado) | Razão Técnica do Value Object |
| :--- | :--- | :--- |
| **WeatherDataId** | Identificador único do registo meteorológico. | **VO:** Garante a imutabilidade da identidade para que o `FlightPlan` possa referenciar um boletim específico. |
| **WeatherReading** | "Air density depends on altitude and temperature." [6.1] | **VO:** Agrupa todas as variáveis físicas num "instantâneo" atmosférico. Ao estar numa lista, permite modelar diferentes condições em diferentes altitudes. |
| **Altitude** | Essencial para indexar as leituras. | **VO:** Garante que os cálculos de densidade e vento são aplicados no nível de voo correto, evitando erros de unidade (Metros vs Pés). |
| **Precipitation (Enum)** | Afeta a segurança e visibilidade. | **Enum:** Define tipos discretos de meteorologia severa que podem impedir a operação em certas áreas. |
| **Date (LocalDateTime)** | A meteorologia é sensível ao tempo. | **Atributo:** Garante que a simulação utiliza dados atuais e não obsoletos, respeitando a validade dos boletins (METAR/TAF). |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. WeatherData (1) *-- (1..*) WeatherReading**
* **Tipo:** Composição.
* **Justificação:** O enunciado refere que a densidade do ar e o vento variam com a altitude [6.1].
* **Razão:** Ter uma lista de leituras permite ao sistema simular uma **coluna de ar real**. O avião pode ter vento de cauda a 10.000m e vento de frente a 2.000m, tornando a simulação de consumo muito mais precisa.

#### **B. WeatherData (1) --> (1) AirControlAreaId**
* **Tipo:** Referência por ID.
* **Justificação:** Os dados meteorológicos são específicos de uma região geográfica controlada.
* **Razão:** Garante que o sistema de simulação sabe exatamente quais as condições que afetam um avião quando este entra nos limites de uma `AirControlArea`.

---

### 3. Justificação dos Métodos de Domínio (Business Logic)

| Método | Justificação Técnica e Física |
| :--- | :--- |
| **`addReading`** | Garante a integridade do agregado ao permitir apenas a inserção de leituras coerentes (ex: não permitir duas leituras para a mesma altitude exata). |
| **`getReadingForAltitude`** | **O "Cérebro" da Simulação:** Este método implementa a lógica de procura (ou interpolação). Ele fornece ao motor da aeronave os valores exatos de $\rho$ (densidade) e vento necessários para calcular a `ground speed` e o `thrust` no momento atual [6.1]. |

---

# Airport Done

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Domínio/Negócio) | Papel na Simulação |
| :--- | :--- | :--- |
| **IATACode / ICAOCode** | Identificadores universais da aviação. | Garante que o aeroporto é reconhecido tanto por sistemas comerciais (IATA) como por sistemas de controlo de tráfego (ICAO). |
| **Coordinates** | Localização exata no globo. | Essencial para o cálculo de distâncias entre aeroportos e para posicionar o aeroporto dentro de uma `AirControlArea`. |
| **Elevation** | Altitude do aeroporto (MSL). | **Input Crítico [6.1]:** Define a pressão atmosférica base para o cálculo da densidade do ar ($\rho$) na descolagem e aterragem. |
| **RunwayConfiguration** | Infraestrutura física das pistas. | Define se o aeroporto tem capacidade para receber modelos específicos de aeronaves com base no comprimento da pista. |
| **OperationalStatus** | Estado de disponibilidade. | Age como um filtro de segurança: impede o planeamento para aeroportos em manutenção ou fechados por mau tempo. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. Airport (1) *-- (1..*) RunwayConfiguration**
* **Tipo:** Composição (Set).
* **Justificação:** Representa o facto de um aeroporto ser um complexo que pode ter várias pistas. Usar um `Set` garante que não existem pistas duplicadas com as mesmas características, mantendo a integridade dos dados.

#### **B. Airport (1) --> (1) AirControlAreaId**
* **Tipo:** Referência por ID.
* **Justificação:** Estabelece a jurisdição. Todo o aeroporto opera sob o comando de uma área de controlo (AR). A referência por ID segue as boas práticas de DDD para evitar o acoplamento direto entre agregados grandes.

---

### 3. Justificação dos Métodos de Domínio

| Método | Lógica Aplicada |
| :--- | :--- |
| **`canHandle(model)`** | Cruza o `requiredRunwayLength` do avião com o `length` das pistas disponíveis no `Set`. Se nenhuma pista chegar, o voo é recusado. |
| **`isOpenFor(weather)`** | Implementa os "mínimos operacionais". Verifica se a visibilidade no `WeatherData` é compatível com o `OperationalStatus` atual do aeroporto. |
| **`getDistanceTo(dest)`** | Utiliza a fórmula de Haversine (ou similar) sobre os VOs de `Coordinates` para determinar a distância real de voo. |

---

# AirTransport Company Done 

### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Domínio / Enunciado) | Razão Técnica do Value Object |
| :--- | :--- | :--- |
| **CompanyId** | Identificador único da companhia no sistema. | **VO:** Garante que a identidade da empresa é imutável e pode ser referenciada por outros agregados (ex: `Aircraft`, `FlightRoute`). |
| **IATACode** | Designador de 2 letras (ex: "TP"). | **VO:** Valida o formato padrão da *International Air Transport Association*. Essencial para identificação comercial de voos. |
| **ICAOCode** | Designador de 3 letras (ex: "TAP"). | **VO:** Valida o formato da *International Civil Aviation Organization*. Crucial para planos de voo e comunicações de controlo aéreo. |
| **AirOperatorCertificate** | Número da licença de exploração aérea (AOC). | **VO:** Garante que a companhia possui autorização legal para operar. Permite validar o formato do certificado antes de permitir operações. |
| **CollaboratorIds** | Lista de utilizadores autorizados (`Set<UserId>`). | **Referência:** Estabelece quem tem permissão para gerir a frota ou submeter planos de voo em nome da empresa, sem acoplar o objeto `User` inteiro. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. AirTransportCompany (1) --> (0..*) AircraftId**
* **Tipo:** Referência por ID.
* **Justificação:** Representa a **Frota** da companhia.
* **Razão:** A multiplicidade `0..*` permite que uma companhia seja criada no sistema antes de adquirir aeronaves. A referência por ID segue as boas práticas de DDD, evitando que o Agregado da Companhia se torne demasiado "pesado" ao carregar todos os dados técnicos dos aviões.

#### **B. AirTransportCompany (1) --> (0..*) UserId**
* **Tipo:** Referência por ID (Set).
* **Justificação:** Define os colaboradores (Despachantes, Gestores) da empresa.
* **Razão:** O uso de um `Set` garante que um utilizador não é adicionado em duplicado. Esta ligação é fundamental para a **segurança do sistema**, garantindo que apenas pessoal autorizado opera os ativos da companhia.

---

### 3. Justificação dos Métodos de Domínio (Business Logic)

| Método | Responsabilidade e Regra de Negócio |
| :--- | :--- |
| **`addCollaborator`** | Gere o acesso à operação da companhia. Valida se o utilizador já existe e se pode ser associado a esta entidade. |
| **`addAircraft`** | Regista a posse de uma aeronave. No contexto de simulação, esta lista é consultada para garantir que o avião escalado para um voo pertence legalmente à frota da empresa. |
| **`listActiveCollaborators`** | Fornece a lista de utilizadores com permissões atuais, permitindo auditoria e controlo sobre quem pode submeter planos de voo. |

---

# FlighrRoute done

### **Justificação do Agregado: FlightRoute**

O agregado **FlightRoute** funciona como o "template" operacional que define a ligação entre dois pontos geográficos. Ele separa a definição estática do serviço (a rota) da sua execução dinâmica (o voo).

#### **1. Value Objects (VO) e Responsabilidades**
* **FlightRouteName:** Garante a conformidade com o padrão IATA (ex: TP1234). Ao ser um VO, valida logo na criação que o nome da rota segue as regras da indústria.
* **Distance & Duration:** Estes VOs encapsulam os dados fundamentais para o **requisito [6.1]**. A `Distance` é o input para o cálculo de combustível, enquanto a `Duration` permite prever a ocupação da aeronave e do espaço aéreo.
* **Active / ActiveUntil:** Introduz uma dimensão temporal. Permite que o sistema faça gestão de rotas sazonais ou descontinue rotas sem apagar o histórico de voos já realizados.

#### **2. Regras de Negócio e Validações de Domínio**
* **Consistência Geográfica:** O Agregado garante que `originAirportId` e `destinationAirportId` são referências válidas e distintas, impedindo rotas logicamente impossíveis (origem igual ao destino).
* **Cálculo de Performance (`calculateRequiredFuel`):** O método de domínio utiliza a `Distance` interna e cruza-a com os parâmetros de consumo do `AircraftModel`. Isto centraliza a lógica de "custo de voo" na entidade que melhor conhece o percurso.
* **Compatibilidade de Aeronave (`isCompatible`):** Antes de permitir que um avião seja alocado a uma rota, o Agregado verifica se a autonomia (*range*) e as capacidades técnicas da aeronave são suficientes para a distância e duração da rota.

#### **3. Integridade Referencial (DDD)**
* A utilização de **IDs** (`CompanyId`, `AirportId`) em vez de objetos completos evita o acoplamento excessivo entre agregados. Isto permite que os aeroportos ou as companhias evoluam (mudança de nome, status, etc.) sem corromper a definição da rota.

---

# Flight plan Done 

### 1. Justificação dos Value Objects (VO) e Entidades Internas

| Elemento | Justificação (Domínio / Enunciado) | Papel no Requisito [6.1] e [3.1] |
| :--- | :--- | :--- |
| **FlightDesignator** | Identificação comercial do voo (ex: TP123). | **VO:** Garante que o voo segue o padrão IATA. Essencial para a legibilidade nos relatórios de simulação. |
| **FlightLeg (Entity)** | Representa cada "perna" ou trecho do voo. | **Entidade:** Permite que um plano de voo tenha múltiplas escalas. Cada trecho é validado individualmente contra uma `AirControlAreaId`. |
| **Payload** | Manifesto de carga e passageiros. | **Peso Crítico:** Fornece a massa comercial ($m_{payload}$). Sem este VO, o simulador não conseguiria calcular a força de sustentação e o arrasto necessários para o consumo de combustível. |
| **FuelPlan** | Planeamento detalhado de combustível. | **Energia:** Divide o combustível em `tripFuel` (consumo) e `reserveFuel` (segurança). Permite ao simulador abater o peso do combustível consumido dinamicamente. |
| **Altitude** | Altitude de cruzeiro solicitada. | **Slot Vertical:** Responde ao requisito **[3.1]** de reserva de "altitude slots" nas áreas de controlo, evitando conflitos de tráfego. |
| **FlightStatus** | Ciclo de vida do plano (Draft, Validated). | **Workflow:** Garante que apenas planos que passaram pelas validações de segurança (peso/combustível) podem ser executados pelo simulador. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. FlightPlan (1) *-- (1..*) FlightLeg**
* **Tipo:** Composição Interna.
* **Justificação:** Um plano de voo é composto por um ou mais trechos. A composição garante que, se o plano for cancelado ou removido, todas as suas "pernas" e reservas de espaço aéreo associadas desaparecem com ele, mantendo a integridade do sistema.

#### **B. FlightPlan (1) --> (1) AircraftId / FlightRouteId / UserId**
* **Tipo:** Referências por ID.
* **Justificação:** Segue as boas práticas de DDD para evitar o acoplamento de agregados massivos. O `FlightPlan` conhece "quem" voa (`Aircraft`), "por onde" voa (`Route`) e "quem" é o responsável (`Pilot/User`), sem precisar de carregar toda a estrutura técnica dessas entidades em memória.

---

### 3. Justificação dos Métodos de Domínio (Business Logic)

| Método | Responsabilidade Técnica e Regra de Negócio |
| :--- | :--- |
| **`calcTotalWeight()`** | **Física [6.1]:** Soma o *Basic Empty Weight* do `Aircraft`, o peso do `Payload` e o peso do combustível no `FuelPlan`. Este valor é o input principal para o motor de física da simulação. |
| **`validateAgainstAircraft()`** | **Segurança:** Cruza o `passengerCount` do `Payload` com a `CabinConfiguration` do avião. Impede, por exemplo, que se vendam 200 bilhetes para um avião de 150 lugares. |
| **`totalFuel()`** | **Logística:** Garante que a soma de todas as parcelas de combustível (Taxi, Trip, Reserve) não excede a capacidade física dos depósitos (`FuelTankCapacity`) da aeronave selecionada. |
| **`attachWeatherData()`** | **Contexto:** Associa o cenário meteorológico ao plano, permitindo que o `FuelPlan` seja ajustado em função do vento previsto (Wind Correction Angle). |

---

# User Done 


### 1. Justificação dos Value Objects (VO) e Atributos

| Elemento | Justificação (Domínio / Enunciado) | Papel na Simulação |
| :--- | :--- | :--- |
| **SecurityClearance** | Níveis de acesso a áreas restritas. | **Controlo [3.1]:** Impede que utilizadores sem autorização visualizem slots de altitude ou submetam planos em áreas de segurança elevada. |
| **License** | Certificação legal do profissional. | **Validação:** O `expiryDate` garante que o sistema rejeita planos de voo associados a pilotos com licenças caducadas. |
| **AircraftModelId** | Habilitação técnica (*Type Rating*). | **Física [6.1]:** Garante que o piloto tem formação específica para o modelo de aeronave que vai operar no simulador. |
| **AirTransportCompanyId**| Vínculo empregatício/operacional. | **Segurança [3.1]:** Define a qual frota e rotas o utilizador tem permissão para aceder ou gerir. |
| **Role (Enum)** | Papéis de responsabilidade. | **Lógica:** Separa as capacidades de quem cria o plano (`PILOT`/`FCO`), quem valida (`ATCC`) e quem fornece dados (`WEATHER_PERSON`). |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. User (1) *-- (1..*) Role**
* **Justificação:** Um utilizador pode acumular funções (ex: um `PILOT` que também é `FCO` na sua companhia). Isto torna o sistema flexível para pequenas transportadoras aéreas.

#### **B. User (1) *-- (0..*) AircraftModelId**
* **Justificação:** Representa as habilitações do piloto. Um piloto pode estar certificado para voar múltiplos modelos (ex: A320 e A330). A multiplicidade `0..*` permite que utilizadores de `BACKOFFICE` ou `ADMIN` não precisem de ter habilitações de voo.

#### **C. User (1) *-- (1..*) AirTransportCompanyId**
* **Justificação:** Embora comummente um piloto trabalhe para uma empresa, consultores ou operadores de aeródromo podem estar vinculados a várias entidades para gestão de rede.

---

### 3. Justificação dos Métodos de Domínio (Business Logic)

| Método | Responsabilidade e Regra de Negócio |
| :--- | :--- |
| **`renewClearance()`** | Mantém a conformidade de segurança do utilizador para acesso às `AirControlArea`. |
| **`assignRole()`** | Gere a evolução do utilizador no sistema, permitindo-lhe assumir novas responsabilidades operacionais. |
| **`recordSkillsAssessment()`** | Justifica-se para efeitos de auditoria e prontidão, assegurando que o piloto mantém as competências para a simulação física [6.1]. |

---

### 4. Dicas de Polimento para o Diagrama (Pequenos detalhes)

1.  **Tipo da Data na Licença:** No VO `License`, tens `expiryDate : String`. Recomendo mudar para **`LocalDate`** (como fizeste na `SecurityClearance`) para permitir cálculos de comparação automática (ex: `licence.isExpired()`).
2.  **Escrita (Typos):** Tens `licenceNumber` (Inglês Britânico) e `License` (Inglês Americano). Recomendo manter a consistência em **`License`**. Tens também `habilitationType` — na aviação chama-se tecnicamente **`ratings`**.

--- 


# Simulation Done


### 1. Justificação dos Value Objects (VO) e Entidades Internas

| Elemento | Justificação (Domínio / Enunciado) | Papel nos Requisitos [3.2] e [6.1] |
| :--- | :--- | :--- |
| **FlightSimulationResult** | Consolida o desfecho individual de cada voo. | **Resultado Final:** Guarda o `actualFuelConsumed` calculado pelo motor de física, permitindo comparar a eficiência real com o planeado no `FlightPlan`. |
| **SimulationSnapshot** | Registo de "caixa-preta" (Black Box) do voo. | **Passo a Passo [6.1]:** Regista o estado da aeronave (`fuelRemaining`, `speed`, `position`) em cada intervalo de tempo, garantindo que o movimento foi simulado de forma contínua. |
| **SafetyViolation** | Registo detalhado de incidentes de segurança. | **Auditoria [3.2]:** Captura o instante exato, o local (`Coordinates`) e a altitude de uma infração (ex: colisão ou invasão de área restrita). |
| **ViolationType (Enum)** | Classificação técnica da falha de segurança. | **Categorização:** Distingue entre falhas de separação mínima (`SEPARATION`), falta de combustível (`FUEL_STARVATION`) ou violação de espaço aéreo (`AREA_INCURSION`). |
| **Coordinates / Altitude** | Dados espaciais precisos do evento. | **Geolocalização:** Define o ponto exato $P(x, y, z)$ onde ocorreu uma violação ou onde se encontra a aeronave num snapshot. |

---

### 2. Justificação das Ligações e Multiplicidades

#### **A. SimulationReport (1) *-- (0..*) FlightSimulationResult**
* **Tipo:** Composição.
* **Justificação:** Um relatório pode conter múltiplos resultados de voo (simulação de tráfego denso numa área). A composição garante que os resultados de voo não existem sem o relatório de contexto que os gerou.

#### **B. FlightSimulationResult (1) *-- (1..*) SimulationSnapshot**
* **Tipo:** Composição Temporal.
* **Justificação:** Para cumprir o requisito **[6.1]**, o sistema deve registar o progresso do voo. Esta multiplicidade permite reconstruir a trajetória completa da aeronave, passo a passo, desde a descolagem até à conclusão ou incidente.

#### **C. SafetyViolation (1) --> (0..*) FlightPlanId**
* **Tipo:** Referência por ID.
* **Justificação:** Permite identificar quais os voos envolvidos numa violação (ex: dois voos numa colisão) sem acoplar o relatório aos objetos dinâmicos do plano de voo, mantendo a integridade histórica.

---

### 3. Justificação dos Métodos de Domínio (Business Logic)

| Método | Responsabilidade Técnica e Regra de Negócio |
| :--- | :--- |
| **`addFlightResult()`** | Agrega o resultado individual de um voo ao relatório final da área de controlo. |
| **`recordViolation()`** | Regista um evento crítico detetado pelo simulador, associando-lhe o tipo de infração e os dados espaciais no momento exato. |
| **`finalise()`** | Fecha o relatório e determina o `overallResult` (PASS/FAIL). Um relatório pode ser `FAIL` se houver uma única `SafetyViolation`, mesmo que todos os voos tenham chegado ao destino. |

---

