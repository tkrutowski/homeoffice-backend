# Monitoring (Grafana Alloy → Grafana Cloud)

Alloy działa w osobnym kontenerze obok backendu, co 30 s zbiera `http://localhost:8077/actuator/prometheus`
(z nagłówkiem `X-Internal-Token`) i wysyła metryki do Grafana Cloud (Hosted Prometheus).
Działa niezależnie od deployu backendu - workflowy GitHub Actions go nie dotykają.

## Instalacja na hoście (Synology / EC2)

1. Skopiuj katalog `monitoring/` na host (np. `/volume2/docker/monitoring` albo `/opt/monitoring`).
2. Utwórz `.env` na podstawie `.env.example`:
   - `APP_INSTANCE` - `synology` albo `ec2` (ta sama wartość co w backendzie na tym hoście),
   - `GRAFANA_CLOUD_API_TOKEN` - Grafana Cloud → Administration → Access policies → Add token,
     scope `metrics:write`,
   - `INTERNAL_API_TOKEN` - ta sama wartość co sekret backendu.
3. `docker compose up -d` (jeśli brak wtyczki compose, np. na EC2: odpowiednik `docker run` z
   `--network host`, `--env-file .env`, tymi samymi wolumenami i komendą jak w `docker-compose.yml`).
4. Sprawdź: `docker logs alloy` (brak błędów `401`/`remote_write`), potem w Grafanie → Explore →
   zapytanie `up{job="homeoffice"}` powinno pokazać wartość `1` dla `instance="synology"`/`"ec2"`.

Zmiana `config.alloy`: edytuj plik na hoście i `docker compose restart alloy`.

## Dlaczego tak

- `network_mode: host` - backend publikuje port 8077 na hoście, więc `localhost:8077` działa tak samo na
  Synology (sieć `home`) i EC2 (domyślny bridge bez DNS po nazwie kontenera). Interfejs Alloya jest
  przypięty do `127.0.0.1:12345`.
- `instance` w etykiecie targetu - bez tego `up` miałoby `instance="localhost:8077"` na obu hostach i alert
  "synology nie działa" byłby niemożliwy.
- Filtr w `prometheus.relabel` odcina histogramy dla zasobów statycznych i `NOT_FOUND` (boty), bo limit
  Grafana Cloud Free to 10k aktywnych serii. Liczbę serii sprawdzisz w Grafanie: *Usage → Metrics*.
- Limit pamięci `256m` / `GOMEMLIMIT=150MiB` - na Synology jest ~1 GB wolnego RAM-u wspólnego z backendem
  i n8n.
