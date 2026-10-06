# Rotina para rodar localmente (deve ser feito via CLI)

```
cd ~/Documents/ws-personal/gestao-frotas
```

# Banco

```
docker compose up -d postgres
docker compose ps
```

# Backend

```
cd backend
./mvnw spring-boot:run
```
