# 🎯 HorusBounties

Plugin de recompensas (*bounties*) de alto rendimiento y moderno para servidores de Minecraft **Paper / Leaf 1.21+**, desarrollado en **Kotlin** con **JDK 21** y **Gradle**.

---

## ⚡ Características Principales

- 💰 **Integración con Vault**: Cobro automático al colocar recompensas y acreditación al asesino al eliminarlas.
- 🗄️ **Almacenamiento Asíncrono Híbrido**: Soporte para **SQLite** (por defecto) y **MariaDB / MySQL** mediante un pool de conexiones optimizado con **HikariCP** y ejecución asíncrona sobre *Virtual Threads* de Java 21.
- 📖 **Menú GUI Paginado (`/bounties`)**: Interfaz visual interactiva con cabezas personalizadas (`PLAYER_HEAD`) de los jugadores ordenadas automáticamente de mayor a menor recompensa.
- 🔥 **Recompensas Automáticas por Killstreak**: Si un jugador alcanza una racha de bajas configurable (ej. 5 bajas), el plugin le asigna automáticamente una recompensa sobre su cabeza.
- 🛡️ **Protección Anti-Boost Avanzada**:
  - Detección y bloqueo por misma dirección IP (evita multicuentas/autocobros).
  - Cooldowns configurables entre parejas de UUID (`asesino <-> víctima`) y parejas de IP.
  - Limpieza automática en memoria de registros expirados.
- 🎨 **Soporte Completo de MiniMessage**: Textos con degradados (*gradients*), colores hexadecimales y tags modernos de Adventure en todos los mensajes y la GUI.
- 🧩 **Expansión PlaceholderAPI**: Placeholders dinámicos para tablas de clasificación, marcadores y hologramas.

---

## 📦 Requisitos y Dependencias

- **Servidor**: Paper, Purpur o Leaf 1.21 o superior.
- **Java**: Java 21 (JDK 21 Runtime).
- **Plugins requeridos**:
  - [Vault](https://www.spigotmc.org/resources/vault.34315/) + Un plugin de economía compatible (EssentialsX, EconomyManager, etc.).
- **Plugins opcionales**:
  - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (para soporte de variables y tops).

---

## 🛠️ Comandos y Permisos

| Comando | Descripción | Permiso por Defecto |
| :--- | :--- | :--- |
| `/bounty <jugador> <cantidad>` | Coloca o incrementa la recompensa sobre un jugador descontando el dinero de tu saldo en Vault. | `horusbounties.use` (Todos) |
| `/bounties` | Abre el menú GUI interactivo y paginado con todas las recompensas activas. | `horusbounties.gui` (Todos) |
| `/bountyadmin clear <jugador>` | Elimina por completo la recompensa de un jugador de la base de datos y memoria. | `horusbounties.admin` (OP) |

---

## 🧩 Placeholders (PlaceholderAPI)

| Placeholder | Descripción | Ejemplo de Salida |
| :--- | :--- | :--- |
| `%horusbounties_amount%` | Recompensa activa sobre el jugador actual. | `500.0` o `0` |
| `%horusbounties_top_1_name%` | Nombre del jugador en el puesto #1 del top de recompensas. | `Steve` |
| `%horusbounties_top_1_amount%` | Monto del puesto #1 del top de recompensas. | `15,000.0` |
| `%horusbounties_top_<N>_name%` | Nombre del jugador en el puesto N del top (ej. `top_2_name`, `top_3_name`). | `Alex` |
| `%horusbounties_top_<N>_amount%`| Monto del jugador en el puesto N del top. | `7,500.0` |

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
  # ... más mensajes configurables
```

---

## 🔨 Compilación desde el Código Fuente

Clona el repositorio y compila con Gradle Wrapper usando JDK 21:

```bash
git clone https://github.com/1Shad0w/HorusBounties.git
cd HorusBounties
./gradlew build
```

El artefacto compilado se generará en:
`build/libs/HorusBounties-1.0.0.jar`

---

## 📜 Reglas de Contribución

Consulta [AGENTS.md](AGENTS.md) para conocer las pautas de desarrollo:
- Prohibido hacer push directo a `main`.
- Ramas separadas (`feat/...`, `fix/...`, `docs/...`).
- Verificación obligatoria con `./gradlew build` antes de crear un Pull Request.
