# Requisitos e Critérios de Aceitação — Solicitação, Aprovação, Agendamento, Calendário e Notificação de Viagens

## 1. Visão Geral

Este documento especifica as regras de negócio, estados, permissões e critérios de aceitação para o módulo institucional de **Solicitação, Análise, Agendamento, Calendário da Frota e Notificações de Viagens** da FSPH.

---

## 2. Histórias de Usuário Atendidas

1. **US01 — Solicitar viagem institucional:** informar período (`departureAt` e `expectedReturnAt`), origem, destino e finalidade institucional.
2. **US02 — Acompanhar status da solicitação:** visualizar situação atual (`PENDING`, `APPROVED`, `REJECTED`, `SCHEDULED`, `CANCELED`) e histórico das próprias solicitações.
3. **US03 — Editar ou cancelar solicitação pendente:** permitir alteração dos dados ou desistência pelo solicitante somente enquanto a solicitação ainda não tiver sido analisada (`PENDING`).
4. **US04 — Analisar disponibilidade no período:** consultar veículos e motoristas disponíveis para o intervalo da solicitação antes de decidir.
5. **US05 — Aprovar ou recusar solicitação:** registrar decisão do gestor, exigindo justificativa obrigatória em caso de recusa (`rejectionReason`).
6. **US06 — Agendar viagem aprovada:** reservar veículo e motorista elegíveis para o período da viagem aprovada (`SCHEDULED`).
7. **US07 — Impedir viagens sobrepostas:** bloquear aprovação (quando houver veículo vinculado/pré-alocado) e agendamento caso exista conflito de horário para o mesmo veículo (ou motorista no agendamento).
8. **US08 — Visualizar calendário da frota:** consultar reservas (`APPROVED` com veículo pré-reservado e `SCHEDULED`), bloqueios de manutenção e disponibilidade de veículos em um intervalo de datas.
9. **US09 — Notificar solicitante:** enviar notificação automática (persistida no sistema e, quando habilitado, via e-mail SMTP) sempre que a solicitação for aprovada, recusada ou agendada.
10. **US10 — Filtrar e validar vagas de passageiros:** informar `passengerCount` (`>= 1`) e listar/aceitar apenas veículos com assentos livres suficientes para passageiros (`Vehicle.capacity - 1 >= passengerCount`, reservando 1 assento para o motorista).

---

## 3. Máquina de Estados da Solicitação (`TripRequestStatus`)

- `PENDING`: estado inicial ao criar a solicitação; pode ser editada ou cancelada pelo solicitante (`CANCELED`), ou decidida pelo gestor (`APPROVED` / `REJECTED`).
- `APPROVED`: solicitação deferida pelo gestor (podendo opcionalmente já pré-reservar um veículo disponível no período); aguarda agendamento definitivo com veículo + motorista (`SCHEDULED`).
- `REJECTED`: solicitação indeferida pelo gestor com `rejectionReason` obrigatório; estado final.
- `SCHEDULED`: viagem agendada com veículo e motorista reservados para o período; estado final do fluxo de agendamento.
- `CANCELED`: solicitação cancelada pelo solicitante antes da análise (`PENDING`); estado final.

---

## 4. Regras de Negócio Detalhadas

### RN01 — Período e Sobreposição de Agenda
- Toda solicitação exige `departureAt` e `expectedReturnAt` com `expectedReturnAt > departureAt`.
- Dois intervalos `[startA, endA)` e `[startB, endB)` se sobrepõem quando `startA < endB` **e** `endA > startB` (viagens contíguas onde `endA == startB` não conflitam).
- Conflitos de veículo consideram solicitações com status `APPROVED` (quando `assignedVehicle` já estiver definido) e `SCHEDULED`.
- Conflitos de motorista consideram solicitações com status `SCHEDULED` para o mesmo `assignedDriver`.
- As operações de reserva utilizam bloqueio pessimista (`PESSIMISTIC_WRITE`) sobre o veículo e o motorista para evitar condições de corrida.

### RN02 — Capacidade de Passageiros (`Vehicle.capacity`)
- O campo `capacity` do veículo inclui o assento do condutor.
- A quantidade máxima de passageiros suportada pelo veículo é `availablePassengerSeats = Math.max(0, capacity - 1)`.
- Uma solicitação com `passengerCount` exige `passengerCount >= 1` e só admite veículos com `capacity - 1 >= passengerCount` (tanto na indicação opcional `requestedVehicleId` pelo solicitante quanto na aprovação/agendamento pelo gestor).

### RN03 — Elegibilidade de Veículos e Motoristas
- **Veículo disponível no período:** ativo (`active = true`), fora de manutenção em andamento (`inMaintenance = false` via `MaintenanceAvailabilityGuard`), com vagas suficientes para passageiros (`capacity - 1 >= passengerCount`) e sem reserva sobreposta no período.
- **Motorista disponível no período:** cadastro `Driver` ativo (`active = true`), usuário vinculado ativo (`user.active = true`), CNH ativa (`RecordStatus.ACTIVE`) com validade `expiresOn >= data de retorno previsto` e sem viagem `SCHEDULED` sobreposta no período.

### RN04 — Edição e Cancelamento pelo Solicitante
- Apenas o próprio autor da solicitação (`requester.id == authenticatedUser.id`) pode editar ou cancelar sua solicitação.
- Edição e cancelamento só são permitidos enquanto `status == PENDING`; qualquer tentativa em outros estados retorna `409 Conflict`.

### RN05 — Notificações ao Solicitante
- Eventos geradores:
  - `TRIP_REQUEST_APPROVED`: decisão de aprovação pelo gestor.
  - `TRIP_REQUEST_REJECTED`: decisão de recusa pelo gestor (inclui a justificativa informada).
  - `TRIP_REQUEST_SCHEDULED`: agendamento concluído (inclui placa/modelo do veículo, nome do motorista e período).
- Toda notificação é persistida em `user_notifications` (com suporte a consulta de não lidas e marcação de leitura) e dispara envio opcional por SMTP quando `app.notifications.email.enabled=true`.

---

## 5. Matriz de Permissões Padrão

| Permissão | `EMPLOYEE` | `MOTORISTA` | `GESTOR_FROTA` | `ADMIN` |
| :--- | :---: | :---: | :---: | :---: |
| `TRIP_REQUESTS_WRITE` (solicitar, listar veículos elegíveis, ver/editar/cancelar próprias) | Sim | Não | Sim | Sim |
| `TRIP_REQUESTS_READ` (listar todas as solicitações e calendário da frota) | Não | Não | Sim | Sim |
| `TRIP_REQUESTS_MANAGE` (analisar disponibilidade, aprovar, recusar e agendar) | Não | Não | Sim | Sim |
| `NOTIFICATIONS_READ` (listar e marcar próprias notificações como lidas) | Sim | Sim | Sim | Sim |
