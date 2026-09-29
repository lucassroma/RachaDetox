# Alba — Mira arriba.

App Android: elige cuánto tiempo al día le das a cada app y construye una racha de días.

## Cómo funciona
- Eliges una o varias apps y un tiempo máximo al día (de 5 min a 1 h).
- Un día cuenta para la racha si **todas** las apps se quedan dentro de su límite.
- Los días se cierran a medianoche. Si te pasas, la racha vuelve a 0.
- Avisos: al 80 % del límite, cuando queda 1 minuto y si te pasas.
- Todo se guarda en el móvil; no hay servidor ni cuentas.

## Instalarla en tu móvil
1. Instala Android Studio (developer.android.com/studio).
2. Descomprime este zip, abre Android Studio → Open → elige la carpeta `RachaDetox`.
   Espera a que termine "Gradle sync" (la primera vez tarda varios minutos).
3. En el móvil: Ajustes → Información del teléfono → pulsa 7 veces "Número de compilación".
   Luego Ajustes → Opciones de desarrollador → activa "Depuración USB".
4. Conecta el móvil por USB, acepta el aviso en el móvil y pulsa ▶ Run en Android Studio.
5. En la app: "Dar permiso" → activa Alba en "Acceso a datos de uso" → vuelve.

## Archivos
- `UsageTracker.kt` – mide el tiempo en primer plano de cada app.
- `StreakEngine.kt` – reglas de la racha.
- `MonitorService.kt` – vigila cada 20 s y envía los avisos.
- `Screens.kt` – pantallas.
- `Store.kt` – guardado de datos.
