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
3. `docker compose up -d` (jeśli brak wtyczki compose, np. na EC2, użyj wariantu `docker run` poniżej).
4. Sprawdź: `docker logs alloy` (brak błędów `401`/`remote_write`), potem w Grafanie → Explore →
   zapytanie `up{job="homeoffice"}` powinno pokazać wartość `1` dla `instance="synology"`/`"ec2"`.

### Wariant bez `docker compose` (EC2)

Na EC2 (`sudo docker compose version` → "unknown command") wystarczą dwa pliki w `/opt/monitoring`:
`config.alloy` i `.env` (z `APP_INSTANCE=ec2`, uprawnienia `600`; `docker-compose.yml` jest zbędny).
Komenda w **jednej linii** (po wklejeniu w kilku liniach terminal wykona tylko pierwszą):

```bash
sudo docker volume create alloy-data
sudo docker run -d --name alloy --restart unless-stopped --network host --memory 256m -e GOMEMLIMIT=150MiB --env-file /opt/monitoring/.env -v /opt/monitoring/config.alloy:/etc/alloy/config.alloy:ro -v alloy-data:/var/lib/alloy/data grafana/alloy:v1.20.1 run --server.http.listen-addr=127.0.0.1:12345 --storage.path=/var/lib/alloy/data /etc/alloy/config.alloy
```

Zmiana `.env` albo `config.alloy` wymaga **odtworzenia** kontenera (`sudo docker rm -f alloy` i ponownie `docker run`):
Docker czyta `--env-file` tylko przy tworzeniu kontenera, więc samo `docker restart` zostawi stary token.
Gdy w logach jest `401 ... invalid token`, token Grafany w `.env` jest zły. Test bez wypisywania tokenu:
`set -a; . /opt/monitoring/.env; set +a; curl -s -o /dev/null -w "%{http_code}\n" -u "$GRAFANA_CLOUD_PROM_USER:$GRAFANA_CLOUD_API_TOKEN" -X POST "$GRAFANA_CLOUD_PROM_URL"`
(`400` = token dobry, `401` = zły, `403` = brak scope `metrics:write`).

## Dashboard w Grafanie

`dashboards/homeoffice.json` - gotowy dashboard (status, wersja i uptime per instancja, ruch HTTP, błędy 5xx,
czasy odpowiedzi, JVM, pula Hikari, zadania w tle, błędy w logach). Import: Grafana → *Dashboards → New →
Import* → *Upload dashboard JSON file* → wskaż plik → w polu źródła danych wybierz
`grafanacloud-homeofficeapp-prom`. Zmienna `Instancja` (u góry) przełącza hosty (synology / ec2).

Zapytania są sprawdzone pod kątem składni PromQL (`promtool check rules`), ale nie w żywej Grafanie -
jeśli któryś panel jest pusty, sprawdź w *Explore*, czy metryka istnieje (np. `homeoffice_async_tasks_total`
pojawia się dopiero po pierwszym zakończonym zadaniu, a `jvm_gc_pause_seconds_*` po pierwszym GC).
Zmiany w dashboardzie wyeksportuj z Grafany (*Share → Export → Save to file*) i podmień plik w repo.

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
