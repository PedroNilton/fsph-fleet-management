# Requisitos e Critérios de Aceitação — Abastecimento, Consumo e Rede Credenciada Prime

## 1. Contexto e Escopo

Este documento formaliza o refinamento de requisitos e os critérios de aceitação do módulo de **Abastecimentos, Consumo Médio e Postos da Rede Credenciada Prime** do sistema de gestão de frotas da **FSPH (Fundação de Saúde Parreiras Horta)**, contemplando as histórias de usuário:

1. **Registro de abastecimentos por motorista/operador:** registrar veículo, data, quilometragem (km), litros, valor total, tipo de combustível e posto da rede credenciada Prime para rastrear o consumo por veículo.
2. **Cadastro de postos da rede credenciada Prime:** manter o cadastro padronizado (inclusão, edição, ativação e inativação) dos postos credenciados.
3. **Visualização de consumo médio (`km/L`) por veículo:** apurar o consumo médio de cada veículo e comparar o desempenho energético da frota.
4. **Alertas de consumo fora do padrão:** identificar e sinalizar automaticamente abastecimentos cujo consumo apresente desvio relevante em relação ao histórico do veículo ou aos limites operacionais, permitindo investigação de irregularidades ou falhas mecânicas.

---

## 2. Perfis e Permissões

| Permissão | Perfis Padrão | Capacidades |
|---|---|---|
| `FUEL_STATIONS_READ` | `ADMIN`, `GESTOR_FROTA`, `MOTORISTA` | Consultar postos da rede credenciada Prime (todos ou apenas ativos para seleção no abastecimento) |
| `FUEL_STATIONS_WRITE` | `ADMIN`, `GESTOR_FROTA` | Cadastrar, atualizar, inativar e reativar postos credenciados Prime |
| `FUELINGS_WRITE` | `ADMIN`, `GESTOR_FROTA`, `MOTORISTA` | Registrar novos abastecimentos e consultar os próprios abastecimentos registrados |
| `FUELINGS_READ` | `ADMIN`, `GESTOR_FROTA` | Consultar histórico geral de abastecimentos, ranking/comparativo de consumo médio (`km/L`) da frota e alertas de consumo fora do padrão |

---

## 3. Regras de Negócio

### RN-ABA-01 — Cadastro de Postos da Rede Credenciada Prime
- Cada posto credenciado (`PrimeFuelStation`) deve possuir:
  - `name`: razão social ou nome fantasia (obrigatório, até 150 caracteres);
  - `cnpj`: CNPJ normalizado para 14 dígitos numéricos, único na base (`ux_prime_fuel_stations_cnpj`);
  - `city`: município (obrigatório, até 100 caracteres);
  - `state`: UF com 2 letras maiúsculas (ex.: `SE`);
  - `address`: endereço completo (obrigatório, até 255 caracteres);
  - `active`: indicador de credenciamento ativo (`true` por padrão).
- Postos com histórico de abastecimentos não são removidos fisicamente; são **inativados** (`deactivate`) ou **reativados** (`reactivate`) para preservar a integridade histórica.

### RN-ABA-02 — Registro de Abastecimento
- Campos obrigatórios do abastecimento (`Fueling`):
  - `vehicleId`: deve referenciar um veículo existente e ativo (`active == true`);
  - `stationId`: deve referenciar um posto da rede Prime existente e **ativo** (`active == true`);
  - `fueledOn`: data do abastecimento (`LocalDate`, não futura);
  - `odometerKm`: quilometragem do veículo no ato do abastecimento (`>= 0`);
  - `liters`: volume abastecido em litros (`BigDecimal > 0`, até 3 casas decimais);
  - `totalValue`: valor financeiro total pago (`BigDecimal > 0`, 2 casas decimais);
  - `fuelType`: tipo de combustível (`GASOLINE`, `ETHANOL`, `DIESEL`, `DIESEL_S10`, `CNG`);
  - `registeredBy`: usuário autenticado (motorista ou operador) extraído do contexto de segurança JWT.

### RN-ABA-03 — Evolução Monotônica de Hodômetro e Cálculo de Trecho (`km/L`)
- Sob lock pessimista no veículo (`vehicles.lockById`):
  - `odometerKm` não pode ser menor que o `currentOdometerKm` atual do veículo;
  - Caso já exista abastecimento anterior para o veículo, `odometerKm` deve ser **estritamente maior** que o `odometerKm` do último abastecimento, sendo `previousOdometerKm = ultimoAbastecimento.odometerKm`;
  - Caso seja o primeiro abastecimento do veículo:
    - se `odometerKm > vehicle.currentOdometerKm`, `previousOdometerKm = vehicle.currentOdometerKm`;
    - se `odometerKm == vehicle.currentOdometerKm`, o registro atua como marco inicial de tanque (`previousOdometerKm = null`, `consumptionKmPerLiter = null`);
  - Quando houver `previousOdometerKm` com distância percorrida positiva (`distanceDrivenKm = odometerKm - previousOdometerKm > 0`), calcula-se:
    $$\text{consumptionKmPerLiter} = \frac{\text{distanceDrivenKm}}{\text{liters}}$$
    arredondado para 2 casas decimais (`HALF_UP`), atualizando `vehicle.currentOdometerKm = odometerKm`.

### RN-ABA-04 — Consumo Médio por Veículo e Comparativo da Frota
- O consumo médio consolidado de um veículo é calculado pela média ponderada real dos trechos medidos:
  $$\text{averageKmPerLiter} = \frac{\sum \text{distanceDrivenKm}}{\sum \text{liters}_{\text{medidos}}}$$
- O comparativo da frota retorna, por veículo: placa, modelo, quantidade de abastecimentos, litros totais, gasto financeiro total (`SUM(totalValue)`), distância medida, `averageKmPerLiter`, consumo médio geral da frota (`fleetAverageKmPerLiter`), desvio percentual em relação à frota e quantidade de alertas de anomalia.

### RN-ABA-05 — Detecção de Consumo Fora do Padrão (Alertas)
- No momento do registro, quando `consumptionKmPerLiter` é apurado, o sistema avalia automaticamente se há consumo fora do padrão (`outOfPattern = true`):
  1. **Desvio em relação à média histórica do próprio veículo:** quando o veículo já possui consumo histórico apurado (`referenceAverageKmPerLiter`), marca alerta se:
     - o consumo cair **25% ou mais abaixo** da média histórica do veículo ($\text{consumo} \le 0{,}75 \times \text{médiaHistórica}$), indicando excesso de consumo, vazamento ou possível desvio; ou
     - o consumo subir **50% ou mais acima** da média histórica ($\text{consumo} \ge 1{,}50 \times \text{médiaHistórica}$), indicando possível inconsistência de lançamento de km/litros.
  2. **Limites absolutos de sanidade operacional:** mesmo sem histórico prévio, marca alerta se $\text{consumo} < 2{,}00\text{ km/L}$ ou $\text{consumo} > 35{,}00\text{ km/L}$.
- Todo alerta registra em `anomalyReason` a justificativa objetiva para auditoria do gestor de frota.

---

## 4. Critérios de Aceitação

- **CA-ABA-01 (Cadastro e unicidade de posto Prime):** Dado um gestor com `FUEL_STATIONS_WRITE`, quando cadastrar um posto Prime com CNPJ válido (com ou sem máscara), então o CNPJ deve ser normalizado para 14 dígitos e duplicidades devem retornar `409 Conflict`.
- **CA-ABA-02 (Bloqueio de posto inativo):** Dado um posto Prime inativado, quando um motorista ou operador tentar registrar abastecimento nesse posto, então a API deve rejeitar com `409 Conflict`.
- **CA-ABA-03 (Registro por motorista/operador e avanço de km):** Dado um motorista autenticado com `FUELINGS_WRITE`, veículo ativo e posto Prime ativo, quando registrar abastecimento com km superior ao atual, litros e valor positivos, então o abastecimento deve ser salvo, o hodômetro do veículo atualizado e o consumo do trecho calculado.
- **CA-ABA-04 (Rejeição de regressão ou repetição de km):** Dado um veículo com abastecimento anterior em `20.500 km`, quando tentar registrar novo abastecimento com `<= 20.500 km`, então a API deve rejeitar com `409 Conflict`.
- **CA-ABA-05 (Consumo médio e comparativo da frota):** Dado múltiplos veículos com abastecimentos registrados, quando o gestor consultar o painel/endpoint de consumo da frota, então deve visualizar o `km/L` médio ponderado de cada veículo, gasto acumulado e comparação com a média da frota.
- **CA-ABA-06 (Alerta automático de consumo fora do padrão):** Dado um veículo cuja média histórica é `10,00 km/L`, quando for registrado um abastecimento resultando em `6,50 km/L` (queda de 35%), então o abastecimento deve ser marcado com `outOfPattern = true`, justificativa preenchida e listado em `GET /api/v1/fuelings/alerts`.
