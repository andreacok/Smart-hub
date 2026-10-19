# Smart Hub

Dashboard web per la casa smart, pensata per un tablet a parete, integrata con **Samsung SmartThings**.
Mostra stanze, dispositivi e stato della casa e permette di comandare luci, tapparelle, prese, termostati e scene.

```text
SmartThings  →  backend Spring Boot (token, cache, normalizzazione)  →  API REST  →  frontend React
```

- **Frontend**: React + TypeScript + Vite (nessun SSR), React Query, CSS Modules.
- **Backend**: Java 21 + Spring Boot 3 + Maven, WebClient verso le API SmartThings.
- **Due modalità**: `mock` (casa demo simulata, nessun account necessario) e `live` (account SmartThings reale).
- Il token SmartThings resta **solo nel backend**: il browser parla esclusivamente con le API interne.

---

## Prerequisiti

| Strumento | Versione | Serve per |
|-----------|----------|-----------|
| Node.js + npm | 20 o superiore (consigliato 22) | frontend |
| Java (JDK) | 21 | backend |
| Maven | 3.9+ | backend |
| Docker + Docker Compose | opzionale (Compose v2.24+) | avvio con un solo comando |

Con VS Code: aprendo il progetto vengono suggerite le estensioni Java/Spring (`.vscode/extensions.json`).
Nel pannello *Run and Debug* trovi le configurazioni "Backend (Spring Boot)", "Frontend (Vite)" e
"Smart Hub (backend + frontend)" che avvia entrambi; la configurazione del backend richiede che esista il file `.env`
(basta una copia di `.env.example`).

---

## Avvio rapido in modalità MOCK (senza SmartThings)

La modalità mock è quella di default: il backend simula una casa con 7 stanze e 22 dispositivi
(le tapparelle si muovono davvero, le temperature oscillano, c'è una porta aperta, una batteria scarica
e una videocamera offline per vedere anche gli avvisi).

**Terminale 1 – backend**

```bash
cd backend
mvn spring-boot:run
```

Il backend risponde su http://localhost:8080 (prova http://localhost:8080/api/home).

**Terminale 2 – frontend**

```bash
cd frontend
npm install
npm run dev
```

Apri **http://localhost:5173**. In sviluppo Vite inoltra le chiamate `/api` al backend sulla porta 8080.
Per usare la dashboard da un tablet sulla stessa rete apri `http://<ip-del-pc>:5173`.

Per provare la gestione degli errori dei comandi (rollback dell'aggiornamento ottimistico):

```bash
SMARTTHINGS_MOCK_FAILURE_RATE=0.5 mvn spring-boot:run
```

---

## Modalità LIVE (account SmartThings reale)

Il backend supporta due tipi di credenziali.

| | Personal Access Token (`pat`) | OAuth (`oauth`) |
|---|---|---|
| Configurazione | 2 minuti | ~15 minuti, una volta sola |
| Durata | **24 ore** per i token creati dopo il 30/12/2024 | rinnovo automatico |
| Uso consigliato | prima prova, sviluppo | hub sempre acceso |

### Opzione A – Personal Access Token (per provare subito)

1. **Cosa creare**: vai su https://account.smartthings.com/tokens, accedi con l'account Samsung e premi
   *Generate new token*.
2. **Permessi da selezionare**:
   - *Devices*: List all devices, See all devices, Control all devices
   - *Locations*: See all locations
   - *Scenes*: See all scenes, Control all scenes (facoltativo: senza, la sezione Scene resta vuota)
3. **Dove inserirlo**: nel file `.env` nella root del progetto, che resta solo sul tuo computer
   (è in `.gitignore`). Il backend lo legge da solo, sia con `mvn spring-boot:run` sia da VS Code e Docker:

   ```bash
   cp .env.example .env
   ```
4. **Variabili da impostare** nel `.env`:

   ```properties
   SMARTTHINGS_MODE=live
   SMARTTHINGS_AUTH=pat
   SMARTTHINGS_TOKEN=il-tuo-token
   # facoltativo, se hai più "case" nell'account:
   SMARTTHINGS_LOCATION_ID=
   ```

   Poi `cd backend && mvn spring-boot:run`. In alternativa puoi esportare le stesse variabili nel terminale
   (hanno la precedenza sul file `.env`).

5. **Verifica la connessione**: apri http://localhost:8080/api/system/status

   ```json
   { "mode": "live", "connected": true, "locationName": "Casa", "roomCount": 6, "deviceCount": 18 }
   ```

   Se `connected` è `false`, il campo `lastError` spiega il motivo (token scaduto, permessi mancanti, rete).
   Anche il log del backend riporta ogni errore SmartThings.

Ricorda che il token scade dopo 24 ore: per un uso continuativo passa a OAuth.

### Opzione B – OAuth con rinnovo automatico (consigliato)

L'access token OAuth dura circa 24 ore e il backend lo rinnova da solo con il refresh token,
salvandolo su file (`SMARTTHINGS_OAUTH_TOKEN_STORE`, in Docker nel volume `smarthub-data`).

1. **Installa la SmartThings CLI** (https://github.com/SmartThingsCommunity/smartthings-cli/releases;
   su macOS `brew install smartthingscommunity/smartthings/smartthings`).
2. **Crea una OAuth-In SmartApp**:

   ```bash
   smartthings apps:create
   ```

   - tipo: *OAuth-In App*
   - scope: `r:locations:*`, `r:devices:*`, `x:devices:*`, `r:scenes:*`, `x:scenes:*`
   - redirect URI: se il backend è raggiungibile in HTTPS usa `https://<tuo-host>/api/auth/smartthings/callback`;
     altrimenti usa un indirizzo come `https://httpbin.org/get` e incolla il codice a mano (punto 5).
   - salva **Client ID** e **Client Secret**: vengono mostrati una sola volta.
3. **Variabili da impostare** (in `.env` o nell'ambiente):

   ```bash
   SMARTTHINGS_MODE=live
   SMARTTHINGS_AUTH=oauth
   SMARTTHINGS_OAUTH_CLIENT_ID=...
   SMARTTHINGS_OAUTH_CLIENT_SECRET=...
   SMARTTHINGS_OAUTH_REDIRECT_URI=https://httpbin.org/get   # lo stesso registrato nella app
   ```

4. **Autorizza**: avvia il backend e apri nel browser http://localhost:8080/api/auth/smartthings/login.
   Accedi con l'account Samsung e consenti l'accesso.
5. Se il redirect URI è il callback del backend, hai finito. Se è `httpbin.org`, copia il valore `code` dalla
   pagina e invialo al backend:

   ```bash
   curl -X POST http://localhost:8080/api/auth/smartthings/code \
        -H 'Content-Type: application/json' -d '{"code":"IL-CODICE"}'
   ```

6. **Verifica**: http://localhost:8080/api/auth/smartthings/status deve indicare `"authorized": true`
   e http://localhost:8080/api/system/status `"connected": true`.

### Tipi di dispositivo

Il backend deduce il tipo (luce, presa, tapparella, termostato, sensore, videocamera) dalla categoria
SmartThings e dalle capability. Se un dispositivo è classificato male (tipico: un relè "Switch" che comanda
una luce) puoi forzarlo in `backend/src/main/resources/application.yml`:

```yaml
smartthings:
  device-type-overrides:
    "id-del-dispositivo": LIGHT
```

L'id si legge da `GET /api/devices`.

---

## Docker

```bash
cp .env.example .env      # facoltativo: senza .env parte in modalità mock
docker compose up --build
```

Apri **http://localhost:8088** (porta modificabile con `SMARTHUB_PORT`).

- `frontend`: build Vite servita da Nginx, che inoltra `/api` al backend (stessa origine, niente CORS).
- `backend`: Spring Boot, non esposto all'esterno. I dati runtime (token OAuth SmartThings e foto personalizzate
  delle stanze) sono nel volume `smarthub-data`, montato su `/data`: sopravvivono alla ricreazione dei container
  e al `docker compose up --build`.
- Le immagini base sono multi-architettura (amd64 e arm64): funzionano su mini PC, NAS, server Linux
  e Raspberry Pi con sistema operativo a 64 bit (Raspberry Pi 4/5 consigliati).

Per aggiornare dopo un `git pull`: `docker compose up --build -d`.

### Migrazione dal volume precedente

Fino a questa versione il backend salvava il token OAuth in `/app/data`. Ora la cartella dei dati è `/data`
(variabile `SMARTHUB_DATA_DIR`). Se usavi l'OAuth, copia `smartthings-oauth.json` dal vecchio volume prima di
avviare la nuova versione, altrimenti dovrai rifare l'accesso a SmartThings. In modalità mock non serve nulla.

---

## Fotografie delle stanze

Ogni stanza ha una fotografia predefinita, inclusa nell'app. Puoi sostituirla con una tua foto dalla pagina
della stanza: tocca la matita in alto a destra della foto.

- **Priorità**: foto personalizzata → foto predefinita della stanza → immagine generica `default-room`.
- **Identificazione**: la foto è legata all'ID della stanza, non al nome. Se SmartThings rinomina la stanza, la
  foto resta. Il nome serve solo a scegliere la foto predefinita.
- **Dove sono salvate**: in `<SMARTHUB_DATA_DIR>/room-images/` (default locale `./data/room-images/`, in Docker
  `/data/room-images/`). Un file `index.json` collega ogni stanza alla sua foto. Le foto caricate non vanno mai in
  `frontend/src/assets`: quelle sono solo le immagini predefinite dell'app.
- **Formati accettati**: JPEG, PNG, WebP e HEIC/HEIF (foto dell'iPhone). Le foto HEIC e WebP vengono convertite nel
  browser prima dell'invio. Il server accetta JPEG e PNG, e verifica il contenuto reale del file, non il nome o il
  tipo dichiarato.
- **Limiti**: massimo 10 MB per file. Le foto vengono ridimensionate al lato lungo di 2400 px, ricodificate in
  JPEG e private di EXIF e GPS, quindi la posizione del luogo dello scatto non viene salvata.
- **Persistenza**: il volume Docker `smarthub-data` conserva le foto anche dopo il rebuild dei container. In locale
  restano finché non cancelli `backend/data/`.
- **Ripristino**: "Ripristina foto predefinita" elimina solo la foto personalizzata. Gli asset built-in non
  vengono toccati.

Endpoint: `POST /api/rooms/{roomId}/image` (multipart, campo `file`), `GET /api/rooms/{roomId}/image/content`,
`DELETE /api/rooms/{roomId}/image`. Il DTO di ogni stanza include `imageUrl` (versionato con `?v=`) e
`customImage`.

---

## GitHub Actions

Il workflow `.github/workflows/ci.yml` parte a ogni `push` e `pull_request`:

| Job | Cosa fa |
|-----|---------|
| Frontend | `npm ci`, `npm run build` (type-check TypeScript + build Vite), `npm run test` (Vitest) — con cache npm |
| Backend | `mvn -B clean verify` (compilazione, test unitari e test delle API in modalità mock) — con cache Maven |
| Docker | build delle due immagini (senza push), solo se i due job precedenti passano |

---

## API del backend

La documentazione interattiva (Swagger UI) è generata automaticamente da springdoc-openapi:

| | In locale | Con Docker |
|---|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html | http://localhost:8088/swagger-ui.html |
| Specifica OpenAPI (JSON) | http://localhost:8080/v3/api-docs | http://localhost:8088/v3/api-docs |

Da Swagger UI puoi provare tutte le chiamate, compresi i comandi ai dispositivi (sono già pronti esempi per
ogni comando). In modalità live i comandi agiscono sui dispositivi reali. Per disattivare la documentazione:
`SMARTHUB_API_DOCS_ENABLED=false`.

| Metodo | Percorso | Descrizione |
|--------|----------|-------------|
| GET | `/api/home` | Tutto per la home: riepilogo, stanze, avvisi, scene, dispositivi accesi |
| POST | `/api/home/actions/lights-off` | Spegne tutte le luci |
| GET | `/api/locations` | Location attiva |
| GET | `/api/rooms` | Stanze con stato aggregato |
| GET | `/api/rooms/{roomId}` | Stanza con dispositivi e avvisi |
| POST | `/api/rooms/{roomId}/actions/all-off` | Spegne luci e prese della stanza |
| GET | `/api/devices?roomId=` | Dispositivi normalizzati |
| GET | `/api/devices/{deviceId}` | Singolo dispositivo |
| GET | `/api/devices/{deviceId}/status` | Solo stato |
| POST | `/api/devices/{deviceId}/commands` | Comando (vedi sotto) |
| GET | `/api/scenes` | Scene |
| POST | `/api/scenes/{sceneId}/execute` | Esegue una scena |
| GET | `/api/system/status` | Modalità e stato della connessione SmartThings |
| GET | `/actuator/health` | Health check |

Comandi accettati da `POST /api/devices/{id}/commands`:

```json
{ "command": "TURN_ON" }
{ "command": "TURN_OFF" }
{ "command": "SET_BRIGHTNESS", "value": 70 }
{ "command": "SET_COLOR_TEMPERATURE", "value": 2700 }
{ "command": "SET_COLOR", "value": { "hue": 200, "saturation": 80 } }
{ "command": "OPEN" }   { "command": "CLOSE" }   { "command": "STOP" }
{ "command": "SET_POSITION", "value": 60 }
{ "command": "SET_TARGET_TEMPERATURE", "value": 21.5 }
{ "command": "SET_THERMOSTAT_MODE", "value": "heat" }
```

Esempio di dispositivo normalizzato:

```json
{
  "id": "soggiorno-lampadario",
  "name": "Lampadario",
  "roomId": "soggiorno",
  "roomName": "Soggiorno",
  "type": "LIGHT",
  "online": true,
  "state": { "power": "ON", "brightness": 75, "colorTemperature": 3000 },
  "capabilities": ["POWER", "DIMMER", "COLOR_TEMPERATURE"]
}
```

Gli errori hanno sempre la forma `{ "status": 502, "code": "SMARTTHINGS_ERROR", "message": "..." }`.

---

## Come funziona

**Backend** (`backend/src/main/java/com/smarthub`)

```text
config/        properties (env vars), wiring mock/live, CORS
smartthings/
  api/         modello grezzo SmartThings + interfaccia SmartThingsApi
  live/        client WebClient (timeout, retry con backoff, paginazione, 401) e token PAT/OAuth
  mock/        casa demo in memoria, stesso formato delle API reali
  mapping/     DeviceNormalizer (SmartThings → modello normalizzato), CommandTranslator
model/         modello normalizzato (Device, DeviceState, tipi, capability, comandi)
service/       HomeStateCache (snapshot + polling), servizi per casa/stanze/dispositivi/scene
dto/           oggetti esposti dalle API
controller/    REST controller
exception/     gestione centralizzata degli errori
```

- Il frontend legge sempre dalla cache del backend: il suo polling non genera traffico verso SmartThings.
- Il backend rilegge lo stato ogni 5 secondi e la struttura (stanze, dispositivi, scene) ogni 5 minuti.
- Dopo un comando la risposta contiene già lo stato atteso; lo stato reale viene riletto poco dopo
  (e seguito più spesso mentre una tapparella è in movimento).
- Ogni variazione pubblica un `DeviceStateChangedEvent`: è il punto di aggancio per Server-Sent Events,
  WebSocket o webhook SmartThings, quando si vorrà sostituire il polling.

**Frontend** (`frontend/src`)

```text
api/          client HTTP tipizzato e tipi delle API
hooks/        React Query: polling, comandi con aggiornamento ottimistico e rollback
components/   layout, card dispositivo (luci, tapparelle, termostato, sensori, prese), home, stanza
pages/        HomePage, RoomPage
lib/          formattazione, previsione dello stato, raggruppamenti
styles/       design token e stili globali
```

Variabili del frontend (facoltative): `VITE_API_BASE_URL` (default: stessa origine),
`VITE_POLL_INTERVAL_MS` (default 3000), `VITE_API_PROXY` (destinazione del proxy di sviluppo).

---

## Configurazione completa

Tutte le opzioni sono in `.env.example` e `backend/src/main/resources/application.yml`.
Le principali:

| Variabile | Default | |
|-----------|---------|---|
| `SMARTTHINGS_MODE` | `mock` | `mock` o `live` |
| `SMARTTHINGS_AUTH` | `pat` | `pat` o `oauth` |
| `SMARTTHINGS_TOKEN` | | Personal Access Token |
| `SMARTTHINGS_BASE_URL` | `https://api.smartthings.com/v1` | |
| `SMARTTHINGS_LOCATION_ID` | prima location | |
| `SMARTTHINGS_STATUS_REFRESH_INTERVAL` | `5s` | polling stato |
| `SMARTTHINGS_STRUCTURE_REFRESH_INTERVAL` | `5m` | polling struttura |
| `SMARTTHINGS_REQUEST_TIMEOUT` | `10s` | timeout per richiesta |
| `SMARTHUB_OWNER_NAME` | | nome nel saluto |
| `SMARTHUB_DATA_DIR` | `./data` | cartella dei dati runtime (foto stanze, indice). Docker: `/data` |
| `SMARTHUB_API_DOCS_ENABLED` | `true` | Swagger UI e `/v3/api-docs` |
| `SMARTHUB_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | solo sviluppo senza proxy |

Le API del backend non hanno autenticazione: sono pensate per la rete di casa.
Non esporre la porta della dashboard su Internet senza un reverse proxy con autenticazione o una VPN.

---

## Stato delle milestone

- [x] **1. Dashboard + backend + mock**: utilizzabile completamente senza SmartThings.
- [x] **2. Lettura reale** (stanze, dispositivi, stato) — implementata, da validare sul tuo account.
- [x] **3. Controllo reale** (luci, dimmer, tapparelle, termostato, scene) — implementato, da validare sui tuoi dispositivi.
- [x] **4. Installazione domestica con Docker** — compose pronto; Raspberry Pi a 64 bit.

Prossimi passi possibili: notifiche push al frontend (SSE) al posto del polling, webhook SmartThings,
autenticazione della dashboard, modalità "schermo a riposo" per il tablet.
