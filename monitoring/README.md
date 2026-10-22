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

### EC2: Alloy od zera (ściąga)

`ec2.yml` wdraża tylko backend (`homeoffice`). Alloya na EC2 uruchamia się ręcznie, raz. EC2 nie ma wtyczki
`docker compose` (`sudo docker compose version` → "unknown command"), więc używamy `docker run`; potrzebne są tylko
dwa pliki w `/opt/monitoring`: `config.alloy` i `.env`. Poniżej `<HOST>`, `<PORT>` i `<klucz>` to adres, port SSH
i (opcjonalnie) klucz prywatny EC2; `ssh` ma małe `-p`, `scp` duże `-P`. **Komendy wklejaj w jednej linii**
(po wklejeniu w kilku liniach terminal wykona tylko pierwszą, a `docker run` dostanie zero argumentów).

**1. Czy backend na EC2 ma endpoint metryk** (wersja >= 6.5.0; wartość tokenu wpisz w terminalu, nie wklejaj jej
nigdzie indziej):

```bash
read -rs -p "INTERNAL_API_TOKEN: " IT; echo
curl -s -o /dev/null -w "%{http_code}
" -H "X-Internal-Token: $IT" http://localhost:8077/actuator/prometheus
unset IT
```

Oczekiwane `200` (`404`/`401` = backend jest w starej wersji albo token jest zły). Wersję backendu pokaże
`sudo docker ps --format '{{.Names}} {{.Image}}'`.

**2. Katalog i `config.alloy`** (z komputera, z katalogu repo):

```bash
ssh -p <PORT> [-i <klucz>] ubuntu@<HOST> "sudo mkdir -p /opt/monitoring && sudo chown ubuntu /opt/monitoring"
scp -P <PORT> [-i <klucz>] monitoring/config.alloy ubuntu@<HOST>:/opt/monitoring/
```

**3. `.env`** (na EC2; tokeny wpisujesz bez echa, nic nie ląduje w historii powłoki ani w rozmowie):

```bash
read -rs -p "Token Grafany (metrics:write): " GT; echo
read -rs -p "INTERNAL_API_TOKEN: " IT; echo
umask 077
cat > /opt/monitoring/.env <<EOF
APP_INSTANCE=ec2
GRAFANA_CLOUD_PROM_URL=https://prometheus-prod-65-prod-eu-west-2.grafana.net/api/prom/push
GRAFANA_CLOUD_PROM_USER=3683945
GRAFANA_CLOUD_API_TOKEN=$GT
INTERNAL_API_TOKEN=$IT
EOF
unset GT IT
ls -l /opt/monitoring/.env
```

`ls` ma pokazać `-rw-------`. Token Grafany sprawdzisz **przed** zapisem (`400` = dobry, `401` = zły,
`403` = brak scope `metrics:write`): `read -rs -p "Token: " T; echo; curl -s -o /dev/null -w "%{http_code}
" -u "3683945:$T" -X POST https://prometheus-prod-65-prod-eu-west-2.grafana.net/api/prom/push; unset T`.

**4. Uruchomienie:**

```bash
sudo docker volume create alloy-data
sudo docker run -d --name alloy --restart unless-stopped --network host --memory 256m -e GOMEMLIMIT=150MiB --env-file /opt/monitoring/.env -v /opt/monitoring/config.alloy:/etc/alloy/config.alloy:ro -v alloy-data:/var/lib/alloy/data grafana/alloy:v1.20.1 run --server.http.listen-addr=127.0.0.1:12345 --storage.path=/var/lib/alloy/data /etc/alloy/config.alloy
```

Za pierwszym razem Docker pobiera obraz; wypisany długi identyfikator = kontener wystartował.

**5. Weryfikacja:**

```bash
sudo docker ps --filter name=alloy
sudo docker logs alloy --since 3m 2>&1 | grep -iE "warn|error|401|403"
sudo docker stats --no-stream alloy
free -m
```

Logi bez błędów, status `Up`, pamięć Alloya poniżej limitu 256 MiB (tuż po starcie bywa wysoka, sprawdź po
kilku minutach). W Grafanie → Explore: `up{job="homeoffice"}` oraz `count by (instance) ({job="homeoffice"})`
mają pokazać `ec2` (i `synology`).

**Codzienna obsługa:**

| Co | Komenda |
| --- | --- |
| Logi na żywo | `sudo docker logs -f alloy` |
| Zatrzymanie / start | `sudo docker stop alloy` / `sudo docker start alloy` |
| Zmiana `.env` albo `config.alloy` | `sudo docker rm -f alloy`, potem ponownie `docker run` z kroku 4 (`restart` nie wczyta nowego `.env`) |
| Nowa wersja Alloya | zmień tag obrazu w komendzie z kroku 4, `docker rm -f alloy`, `docker run` |
| Usunięcie | `sudo docker rm -f alloy` (bufor `alloy-data` zostaje; `sudo docker volume rm alloy-data` go kasuje) |

Po `docker stop` kontener sam nie wstanie (`--restart unless-stopped` nie wznawia ręcznie zatrzymanych), więc po
teście alertu pamiętaj o `docker start alloy`.

**Typowe problemy:**

- `docker run` bez argumentów / "requires at least 1 argument": komenda wklejona w kilku liniach, wklej w jednej.
- `Conflict. The container name "/alloy" is already in use`: `sudo docker rm -f alloy` i powtórz `docker run`.
- `401 ... invalid token` w logach: zły token Grafany w `.env`; test z kroku 3, potem odtworzenie kontenera.
- W Grafanie tylko `up` i `scrape_*` (około 5 serii) bez metryk aplikacji: backend na EC2 jest w starej wersji
  (bez `/actuator/prometheus`) albo `INTERNAL_API_TOKEN` w `.env` jest inny niż w backendzie; sprawdź krok 1.
- Docker utworzył katalog `/opt/monitoring/config.alloy` zamiast pliku (wgrałeś plik po `docker run`):
  `sudo docker rm -f alloy`, `sudo rmdir /opt/monitoring/config.alloy`, wgraj plik i powtórz `docker run`.
- `ec2.yml` robi `docker system prune -a -f --volumes` przy każdym deployu: obraz i wolumen Alloya przetrwają,
  dopóki kontener istnieje (także zatrzymany). Po `docker rm` następny deploy usunie obraz i bufor `alloy-data`
  (obraz zostanie pobrany ponownie przy `docker run`, utraci się tylko niewysłany bufor metryk).

## Dashboard w Grafanie

`dashboards/homeoffice.json` - gotowy dashboard (status, wersja i uptime per instancja, ruch HTTP, błędy 5xx,
czasy odpowiedzi, JVM, pula Hikari, zadania w tle, błędy w logach). Import: Grafana → *Dashboards → New →
Import* → *Upload dashboard JSON file* → wskaż plik → w polu źródła danych wybierz
`grafanacloud-homeofficeapp-prom`. Zmienna `Instancja` (u góry) przełącza hosty (synology / ec2).

Drugi dashboard, `dashboards/goahead.json` (*HomeOffice - GoAhead (faktury, koszty, KSeF)*), dotyczy modułu
`goahead`: operacje KSeF (logowania odrzucone np. przez wygasły `KSEF_TOKEN`, wyniki i czasy), zadania w tle
(KSeF, PDF, ZUS DRA) i ruch HTTP na `/api/goahead/*`. Importuje się tak samo; oba dashboardy mają link do siebie
(lista *Powiązane dashboardy* u góry).

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
