# AGENTS.md - Reglas del Proyecto HorusBounties

## Flujo de Trabajo y Control de Versiones (Git & GitHub)

1. **Prohibido hacer push directo a `main`**:
   - Nunca ejecutes `git push origin main` ni hagas commits de nuevas funcionalidades, correcciones o refactorizaciones directamente sobre `main`.

2. **Uso obligatorio de ramas por tarea**:
   - Toda modificación debe realizarse en una rama nueva creada a partir de `main`.
   - Convención de nombres para ramas:
     - `feat/...` para nuevas características (ej. `feat/bounty-gui`)
     - `fix/...` para corrección de errores (ej. `fix/anti-boost-cooldown`)
     - `refactor/...` para refactorizaciones y mejoras internas (ej. `refactor/database-pool`)

3. **Integración exclusiva mediante Pull Request (PR)**:
   - Todos los cambios deben integrarse a `main` únicamente a través de un Pull Request en GitHub.

4. **Verificación obligatoria antes de abrir el PR**:
   - Antes de abrir cualquier Pull Request, es obligatorio ejecutar y verificar que `./gradlew build` finalice con éxito (`BUILD SUCCESSFUL`) sin errores de compilación ni fallos en los tests.
