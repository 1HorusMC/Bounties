# 🎯 HorusBounties

Plugin de recompensas (*bounties*) de alto rendimiento y moderno para servidores de Minecraft **Paper / Purpur / Leaf**, desarrollado en **Kotlin** con **JDK 21** y **Gradle**.

---

## 🌐 Compatibilidad de versiones

- **Versiones de Minecraft soportadas:** 1.20.5 hasta 26.3.
- **Entornos / Forks compatibles:** Paper, Purpur, Leaf y derivados modernos basados en Paper API.
- **Entorno de ejecución de Java:** Java 21 (LTS) o superior.

---

## 📦 Requisitos y dependencias

| Dependencia | Tipo | ¿Obligatoria? | Descripción |
|:---|:---:|:---:|:---|
| **Java 21+** | Entorno | **Sí** | Requerido para ejecutar el plugin. Utiliza Virtual Threads. |
| [**Vault**](https://www.spigotmc.org/resources/vault.34315/) | Plugin | **Sí** | Necesario para gestionar las transacciones económicas de las recompensas. |
| **Plugin de economía** | Plugin | **Sí** | Proveedor compatible con Vault, como EssentialsX o EconomyManager. |
| [**PlaceholderAPI**](https://www.spigotmc.org/resources/placeholderapi.6245/) | Plugin | No | Necesario únicamente para utilizar los placeholders del plugin. |
| **MariaDB / MySQL** | Servicio externo | No | Solo necesario si se configura `database.type` como `MARIADB`. Por defecto utiliza SQLite, sin necesidad de servicios externos. |

---

## ⚡ Características principales

- 💰 **Integración con Vault:** cobro automático al colocar recompensas y acreditación del dinero al asesino cuando elimina al objetivo.
- 🗄️ **Almacenamiento asíncrono híbrido:** soporte para **SQLite** (predeterminado) y **MariaDB / MySQL**, mediante un pool de conexiones optimizado con **HikariCP** y ejecución asíncrona sobre Virtual Threads de Java 21.
- 📖 **Menú GUI paginado (`/bounties`):** interfaz visual interactiva con cabezas personalizadas (`PLAYER_HEAD`) de los jugadores, ordenadas automáticamente de mayor a menor recompensa.
- 🔥 **Recompensas automáticas por Killstreak:** asignación automática de recompensas cuando un jugador alcanza una racha de bajas configurable.
- 🛡️ **Protección Anti-Boost avanzada:**
  - Detección y bloqueo de jugadores que comparten dirección IP.
  - Cooldowns configurables entre parejas de UUID (asesino ↔ víctima).
  - Cooldowns configurables entre parejas de IP.
  - Limpieza automática en memoria de registros expirados.
- 🎨 **Soporte completo de MiniMessage:** degradados (*gradients*), colores hexadecimales y etiquetas modernas de Adventure en mensajes y GUI.
- 🧩 **Expansión PlaceholderAPI:** placeholders dinámicos para tablas de clasificación, marcadores y hologramas.

---

## 🛠️ Comandos y permisos

| Comando | Descripción | Permiso por defecto |
|:---|:---|:---|
| `/bounty <jugador> <cantidad>` | Coloca o incrementa una recompensa sobre un jugador, descontando el dinero del saldo del ejecutor mediante Vault. | `horusbounties.use` (Todos) |
| `/bounties` | Abre el menú GUI interactivo y paginado con todas las recompensas activas. | `horusbounties.gui` (Todos) |
| `/bountyadmin clear <jugador>` | Elimina por completo la recompensa de un jugador de la base de datos y de la memoria. | `horusbounties.admin` (OP) |

---

## 🧩 Placeholders (PlaceholderAPI)

| Placeholder | Descripción | Ejemplo de salida |
|:---|:---|:---|
| `%horusbounties_amount%` | Recompensa activa sobre el jugador actual. | `500.0` o `0` |
| `%horusbounties_top_1_name%` | Nombre del jugador en el primer puesto del ranking. | `Steve` |
| `%horusbounties_top_1_amount%` | Importe de la recompensa del primer puesto. | `15,000.0` |
| `%horusbounties_top_<N>_name%` | Nombre del jugador en el puesto N. | `Alex` |
| `%horusbounties_top_<N>_amount%` | Importe de la recompensa del puesto N. | `7,500.0` |

---

## ⚙️ Configuración (`config.yml`)

```yaml
database:
  # Tipos soportados: SQLITE, MARIADB
  type: "SQLITE"

  sqlite:
    file: "bounties.db"

  mariadb:
    host: "localhost"
    port: 3306
    database: "horusbounties"
    username: "root"
    password: ""
    pool-size: 10

bounty:
  min-amount: 100.0
  max-amount: 1000000.0
  allow-self-bounty: false

killstreak:
  enabled: true
  kills-required: 5
  repeat-every-streak: true
  reward-amount: 500.0

anti-boost:
  enabled: true
  block-same-ip: true
  uuid-cooldown-seconds: 1800
  ip-cooldown-seconds: 3600

gui:
  title: "<gradient:#ffaa00:#ff5555><bold>Recompensas</bold></gradient> <dark_gray>(<gray>Página <page>/<max_page></gray>)"
  head-name: "<gold><bold><player></bold></gold>"
  head-lore:
    - "<gray>Recompensa actual: <green>$<amount></green>"
    - "<dark_gray>¡Elimina a este jugador para reclamarla!"
  previous-page-name: "<yellow>« Página Anterior</yellow>"
  next-page-name: "<yellow>Página Siguiente »</yellow>"
  close-name: "<red>Cerrar</red>"

messages:
  prefix: "<gradient:#ffaa00:#ff5555><bold>HorusBounties</bold></gradient> <dark_gray>»</dark_gray> "
  bounty-placed: "<prefix><yellow><sender></yellow> <gray>ha puesto una recompensa de</gray> <green>$<amount></green> <gray>sobre</gray> <gold><target></gold><gray>. (Total: <green>$<total></green>)</gray>"
  bounty-claimed: "<prefix><yellow><killer></yellow> <gray>eliminó a</gray> <red><victim></red> <gray>y reclamó una recompensa de</gray> <green>$<amount></green><gray>!</gray>"
  bounty-killstreak: "<prefix><red><player></red> <gray>alcanzó una racha de</gray> <gold><streak> bajas</gold> <gray>y recibió una recompensa automática de</gray> <green>$<amount></green><gray>! (Total: <green>$<total></green>)</gray>"
  bounty-cleared: "<prefix><gray>Se eliminó la recompensa de</gray> <gold><target></gold><gray>.</gray>"
  anti-boost-blocked: "<prefix><red>Baja ignorada por protección anti-boost (misma IP o cooldown entre jugadores).</red>"
  # ... Más mensajes configurables
```

---

## 🔨 Compilación desde el código fuente

Clona el repositorio y compila utilizando Gradle Wrapper con JDK 21:

```bash
git clone https://github.com/1Shad0w/HorusBounties.git
cd HorusBounties
./gradlew build
```

El artefacto compilado se generará en:

```text
build/libs/HorusBounties-1.0.0.jar
```

---

## 📜 Reglas de contribución

Consulta [`AGENTS.md`](AGENTS.md) para conocer las pautas de desarrollo.

- Está prohibido hacer push directamente a `main`.
- Utiliza ramas independientes según el tipo de cambio (`feat/...`, `fix/...`, `docs/...`).
- La ejecución de `./gradlew build` es obligatoria antes de crear un Pull Request.