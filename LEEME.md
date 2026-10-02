# Alba — Mira arriba.

App Android: elige cuánto tiempo al día le das a cada app y construye una racha de días.

## Cómo funciona
- Eliges una o varias apps y un tiempo máximo al día (de 5 min a 1 h).
- Un día cuenta para la racha si **todas** las apps se quedan dentro de su límite.
- Los días se cierran a medianoche. Si te pasas del límite y no salvas tu racha, la pierdes.
- Para salvarla cierras hasta mañana las apps en las que te has pasado. Solo se puede si no te has pasado más de 15 minutos.
- Una app bloqueada se puede desbloquear; si te habías pasado con ella, la racha deja de estar salvada.
- Si cambias el límite de una app, el cambio no se aplica hasta el día siguiente.
- Si te pasas y no la salvas, el fondo se va oscureciendo hacia gris según te acercas a los 15 minutos, y Alba te avisa (a 10, 5 y 2 minutos del final). Con la racha salvada el fondo es gris muy claro. Al día siguiente vuelve el de siempre.
- Si pierdes la racha, al entrar verás tus días bajando hasta cero.
- Si la pierdes del todo, las apps en las que te has pasado se cierran ese día. Si al día siguiente no las abres, recuperas la racha; si intentas abrirlas, Alba te pregunta: «¿Seguro que quieres seguir? No podrás recuperar tu racha».
- «Cinco minutos más, por favor»: una vez al día, 5 minutos más ese día para todas las apps.
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
