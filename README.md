# Gestão de Frotas

Sistema de gestão de frotas desenvolvido com:

- Java 21;
- Spring Boot;
- Spring Web MVC;
- Spring Data JPA;
- Spring Security;
- Flyway;
- PostgreSQL;
- Docker e Docker Compose.

## Pré-requisitos

Antes de executar o projeto, verifique se estão instalados:

- Git;
- Java 21;
- Docker;
- Docker Compose.

Use os comandos abaixo para verificar:

```bash
git --version
java --version
docker --version
docker compose version
```

Não é necessário instalar o Maven, pois o backend possui o Maven Wrapper (`mvnw`).

## 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd gestao-frotas
```

Substitua `<URL_DO_REPOSITORIO>` pela URL real do projeto.

## 2. Configurar as variáveis de ambiente

Na raiz do projeto, crie o arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

O arquivo deve possuir estas variáveis:

```env
POSTGRES_DB=gestao_frotas
POSTGRES_USER=gestao_frotas
POSTGRES_PASSWORD=sua_senha
POSTGRES_PORT=5432

ADMIN_NAME=Administrador
ADMIN_EMAIL=admin@exemplo.com
ADMIN_PASSWORD=admin_senha
ADMIN_SECTOR_NAME=Administrativo

JWT_SECRET=
JWT_EXPIRATION=PT1H
JWT_ISSUER=gestao-frotas
```

Altere as senhas de exemplo e gere um JWT secret aleatório para o ambiente local.

Na primeira inicialização, o backend cria o administrador configurado acima e o
setor `Administrativo`, caso ainda não existam. Opcionalmente, personalize o nome
do setor com `ADMIN_SECTOR_NAME`. Inicializações posteriores não duplicam o usuário.

> O arquivo `.env` contém informações locais e não deve ser enviado ao Git.

## Autenticação

Envie as credenciais do administrador para obter um token:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@exemplo.com",
  "password": "admin_senha"
}
```

Nas rotas protegidas, envie o token retornado no cabeçalho:

```http
Authorization: Bearer <token>
```

Somente o login e o health check são públicos. As demais rotas exigem um JWT
válido.

## 3. Iniciar o PostgreSQL

Ainda na raiz do projeto, execute:

```bash
docker compose up -d postgres
```

Na primeira execução, o Docker baixará a imagem do PostgreSQL e criará:

- o contêiner do PostgreSQL;
- o banco definido em `POSTGRES_DB`;
- o usuário definido em `POSTGRES_USER`;
- um volume para preservar os dados.

Verifique o status:

```bash
docker compose ps
```

O banco está pronto quando o serviço `postgres` aparecer como `healthy`.

Para acompanhar os logs:

```bash
docker compose logs -f postgres
```

Pressione `Ctrl+C` para sair da visualização dos logs. Isso não encerra o banco.

## 4. Carregar as variáveis para o backend

O Docker Compose lê o `.env` automaticamente, mas o Spring Boot não. Por isso, carregue as variáveis no terminal:

```bash
set -a
source .env
set +a
```

Esse comando deve ser executado na raiz do projeto e repetido sempre que um novo terminal for aberto para iniciar o backend.

## 5. Iniciar o backend

Depois de carregar as variáveis, entre na pasta do backend:

```bash
cd backend
```

Execute a aplicação:

```bash
./mvnw spring-boot:run
```

Na primeira execução, o Maven Wrapper baixará as dependências do projeto. Isso pode levar alguns minutos.

A aplicação estará pronta quando o terminal apresentar uma mensagem semelhante a:

```text
Started GestaoFrotaBackendApplication
```

Por padrão, o backend será iniciado em:

```text
http://localhost:8080
```

## Ordem completa de inicialização

Sempre siga esta ordem:

1. Entrar na raiz do projeto;
2. iniciar o PostgreSQL;
3. confirmar que o banco está saudável;
4. carregar as variáveis do `.env`;
5. entrar na pasta `backend`;
6. iniciar o Spring Boot.

Comandos resumidos:

```bash
cd gestao-frotas

docker compose up -d postgres
docker compose ps

set -a
source .env
set +a

cd backend
./mvnw spring-boot:run
```

## Migrations do banco

As migrations ficam em:

```text
backend/src/main/resources/db/migration/
```

Quando o backend é iniciado, o Flyway procura migrations nessa pasta e aplica automaticamente aquelas que ainda não foram executadas.

Os arquivos devem seguir o padrão:

```text
V1__descricao_da_migration.sql
V2__outra_migration.sql
```

As migrations não precisam ser executadas manualmente.

## Parar o projeto

Para encerrar o backend, pressione `Ctrl+C` no terminal em que ele está sendo executado.

Para parar o PostgreSQL, volte à raiz do projeto e execute:

```bash
docker compose stop postgres
```

Os dados continuarão salvos no volume do Docker.

Para remover o contêiner, preservando os dados:

```bash
docker compose down
```

> Evite utilizar `docker compose down -v`, pois a opção `-v` remove o volume e apaga os dados do banco.

## Rotina diária

Ao começar a trabalhar:

```bash
cd gestao-frotas
docker compose up -d postgres

set -a
source .env
set +a

cd backend
./mvnw spring-boot:run
```

Ao terminar:

1. Pressione `Ctrl+C` para encerrar o backend;
2. volte à raiz do projeto;
3. pare o banco:

```bash
docker compose stop postgres
```

## Comandos úteis

Ver o estado dos serviços:

```bash
docker compose ps
```

Ver em qual porta o PostgreSQL está publicado:

```bash
docker compose port postgres 5432
```

Verificar se a porta `5432` está sendo utilizada:

```bash
sudo ss -ltnp | grep ':5432'
```

Acompanhar os logs do PostgreSQL:

```bash
docker compose logs -f postgres
```

Entrar no banco pelo terminal:

```bash
docker compose exec postgres psql -U gestao_frotas -d gestao_frotas
```

Sair do PostgreSQL:

```text
\q
```

Executar os testes do backend:

```bash
cd backend
./mvnw test
```

## Problemas comuns

### Erro de conexão com o Docker

Se aparecer uma mensagem de permissão ao acessar `/var/run/docker.sock`, verifique se o usuário pertence ao grupo `docker`:

```bash
groups
```

### Porta 5432 ocupada

Verifique qual processo está usando a porta:

```bash
sudo ss -ltnp | grep ':5432'
```

Outra alternativa é alterar `POSTGRES_PORT` no `.env`, por exemplo:

```env
POSTGRES_PORT=5433
```

Depois recrie o contêiner:

```bash
docker compose up -d --force-recreate postgres
```

### Variáveis não reconhecidas pelo Spring Boot

Se o erro mostrar valores como `${POSTGRES_PORT}` ou `${POSTGRES_DB}`, as variáveis não foram carregadas no terminal.

Volte à raiz do projeto e execute:

```bash
set -a
source .env
set +a
```

Depois inicie novamente o backend.
