# CLAUDE.md — putman

Eclipse RCP 4 REST API client (Postman-like), built from scratch.

## Build & Run

- **Run:** Right-click `civerooni.com.putman.ui` → Run As → Eclipse Application
- **Export product:** Open `putman.product` → Export → Eclipse Product
- **Product launcher name:** `Putman`
- **Target platform:** `civerooni.com.putman.product/putman.target` (Eclipse 2024-12 SimRel, JavaSE-21)

## Project Structure

```
civerooni.com.putman.core/    # HTTP client, models, logging
civerooni.com.putman.ui/      # E4 UI: parts, handlers, Application.e4xmi
civerooni.com.putman.feature/ # Feature packaging
civerooni.com.putman.product/ # Product definition (putman.product, putman.target)
civerooni.com.putman.target/  # Target platform placeholder
```

## Application Model

`civerooni.com.putman.ui/Application.e4xmi` — 1100×700 window, horizontal `PartSashContainer`:
- **Collections** (250px, left) — `RequestCollectionPart`
- **Right vertical split:**
  - **Request** (400px, top) — `RequestEditorPart`
  - **Response** (300px, bottom) — `ResponsePart`

Menus: File (New Request, Exit) | Request (Send)
Commands: `exit`, `newrequest`, `sendrequest`

## Data Flow

1. `RequestEditorPart` creates a `RequestModel`, stores it in `IEclipseContext`, syncs it via widget listeners (method `Combo` + URL `Text`)
2. Send button fires `sendrequest` command via `ECommandService` + `EHandlerService`
3. `SendRequestHandler` reads `RequestModel` from context (`@Optional`), calls `PutmanHttpClient.getInstance().send()` (async)
4. Result posted as `ResponseModel` via `IEventBroker` on topic `civerooni/putman/ui/response`
5. `ResponsePart` receives it via `@Inject @Optional @UIEventTopic` and updates status label + body text

## Core Classes

- `PutmanHttpClient` — singleton wrapping Java `java.net.http.HttpClient`; async sends via `CompletableFuture`; 10s connect / 30s request timeouts
- `RequestModel` — holds `HttpMethod` + URL string
- `ResponseModel` — holds status code + body, or error string; factory: `ResponseModel.error(msg)`
- `HttpMethod` — enum: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
- `PutmanLogger` — thin logging wrapper

## Key Handlers

- `SendRequestHandler` — reads `RequestModel` from context, sends HTTP request, posts `ResponseModel` to event broker
- `NewRequestHandler` — resets the request model
- `ExitHandler` — quits the application
