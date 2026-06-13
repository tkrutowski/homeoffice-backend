# Deployment — Production Setup (lokalnie na Docker)

## Architektura

```
http://localhost
       ↓
    Nginx (port 80) — reverse proxy
       ├─ /api/* → Backend (port 8077, internal)
       └─ /* → Frontend (port 3000, internal)
```

Wszystkie serwisy działają w Docker network (`homeoffice-network`) — nie trzeba otwierać portów 8077 i 3000.

---

## Wymagania

- Docker (z Docker Compose)
- Dwa repozytoria obok siebie:
  ```
  C:\Users\Tomek\OneDrive\Programowanie\aktualne\
  ├─ homeoffice-backend\        ← docker-compose.yml tutaj
  └─ homeoffice-frontend\       ← osobne Dockerfile
  ```
- Dostęp do **zdalnej produkcyjnej bazy MySQL** (parametry w `.env`)

---

## Setup

### 1. Przygotuj `.env` plik

```bash
# W homeoffice-backend/
cp .env.example .env
```

Edytuj `.env` — uzupełnij parametry do produkcyjnej bazy:

```env
DB_URL=jdbc:mysql://prod-db-host:3306/homeoffice?serverTimezone=Europe/Warsaw&useSSL=true&requireSSL=true
DB_USERNAME=prod_user
DB_PASSWORD=prod_password
JWT_SECRET_KEY=your_secret_key_here
AWS_ACCESS_KEY_ID=your_key
AWS_SECRET_ACCESS_KEY=your_secret
...
```

### 2. Zbuduj JAR backendu (raz)

```bash
cd homeoffice-backend
mvn clean package -DskipTests
```

Docker weźmie zbudowany JAR z `target/` i umieści go w kontenerze.

### 3. Uruchom Docker Compose

```bash
# W homeoffice-backend/
docker-compose up -d
```

Docker zbuduje:
- **Nginx** (reverse proxy)
- **Backend** (Java, z istniejącego Dockerfile)
- **Frontend** (Vue, z nowego Dockerfile)

Wszystkie serwisy mają `restart: unless-stopped` — uruchamiają się automatycznie po restarcie dockera.

---

## Użytkowanie

### Dostęp do aplikacji

- **Frontend**: http://localhost (port 80)
- **Backend API**: http://localhost/api/* (nginx rozsyła do backendu)
- **Backend health**: http://localhost/actuator/health

### Logi

```bash
# Wszystkie serwisy
docker-compose logs -f

# Konkretny serwis
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f nginx
```

### Zatrzymanie

```bash
docker-compose down
```

Usuwa kontenery, ale obrazy zostają (szybkie restartowanie).

### Usunięcie wszystkiego

```bash
docker-compose down -v
docker rmi homeoffice-backend homeoffice-frontend nginx
```

---

## Aktualizacja aplikacji

### Backend

```bash
# 1. Zaktualizuj kod (git pull)
cd homeoffice-backend
git pull

# 2. Zbuduj nowy JAR
mvn clean package -DskipTests

# 3. Przebuduj obraz i uruchom
docker-compose down
docker-compose up -d --build
```

### Frontend

```bash
# 1. Zaktualizuj kod (git pull)
cd ../homeoffice-frontend
git pull

# 2. Powróć do backendu i przebuduj
cd ../homeoffice-backend
docker-compose down
docker-compose up -d --build
```

---

## Troubleshooting

### Port 80 zajęty

Jeśli inny serwis używa portu 80:

```bash
# Zmień port w docker-compose.yml
# Zmień: ports: - "80:80"
# Na:    ports: - "8080:80"

# Wtedy: http://localhost:8080
```

### Backend nie łączy się z bazą

```bash
# Sprawdź ustawienia .env
cat .env | grep DB_

# Test połączenia z portu hosta
docker exec homeoffice-backend curl http://localhost:8077/actuator/health

# Logi backendu
docker-compose logs backend
```

### Frontend nie ładuje się

```bash
# Sprawdź nginx
docker-compose logs nginx

# Test frontend w kontenerze
docker exec homeoffice-frontend ls /usr/share/nginx/html/dist
```

### Przebudować konkretny serwis

```bash
docker-compose up -d --build backend
docker-compose up -d --build frontend
```

---

## Monitoring

### Status kontenerów

```bash
docker-compose ps
```

### Zużycie zasobów

```bash
docker stats
```

### Health check backendu

Nginx automatycznie sprawdza `/actuator/health` co 30s (timeout 40s po starcie).

---

## Notes

- **Timezone**: Ustawiony na `Europe/Warsaw` w Dockerze i SQL
- **TZ env var**: Ważny dla logiki dat w Javie
- **Baza danych**: Zawsze zdalna (produkcyjna) — nie ma lokalnej MySQL w kontenerze
- **Secrets**: Nie commituj `.env` — dodaj do `.gitignore`

