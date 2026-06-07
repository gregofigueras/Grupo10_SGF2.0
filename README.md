# Grupo10_SGF2.0

Proyecto: Sistema de Gestión de Turnos (SGF) - Iteración 4

## Integrantes

- Carlos Andrés Efstratiadis
- Juan Gaillour
- Martín Patriarca
- Gregorio Figueras

## Resumen

Aplicación Java modular para la gestión y visualización de turnos en un entorno con kioscos, operadores y una pantalla pública. La solución está dividida en módulos independientes (módulos IntelliJ):

- `kiosko`: interfaz para que el cliente solicite un turno (`Kiosco`).
- `Operador`: panel para que el operador atienda y gestione turnos (`PanelOperador`).
- `Pantalla`: aplicación que muestra el turno actual y un historial (`MonitorSala`).
- `ServidorCentral`: núcleo del sistema: persistencia, lógica de colas, renotificaciones y servidor de comunicaciones.

## Persistencia

Se proveen varias implementaciones de persistencia en `ServidorCentral/src/persistencia/`:

- `HistorialLlamadosJSON`, `HistorialLlamadosXML`, `HistorialLlamadosTP` — manejan el historial de llamados.
- `ColaEsperaJSON`, `ColaEsperaXML`, `ColaEsperaTP` — manejan la cola de espera persistente.

Los archivos de persistencia (ejemplos en la raíz del proyecto durante ejecución):

- `historial_llamados.json` (o `.xml` / `.txt` según la implementación activa)
- `cola_espera.json` (o `.xml` / `.txt`)

Las entradas se encriptan mediante la clase `seguridad.Encriptador` (usa `CifradoSimetrico`).

## Dependencias

- Gson (biblioteca para JSON): `ServidorCentral/lib/gson-2.10.1.jar`.

Si el IDE no reconoce `com.google.gson`, verificar la configuración:

- IntelliJ: los `.iml` y `.idea/libraries` ya referencian el jar.
- VS Code: `.vscode/settings.json` incluye `java.project.referencedLibraries` apuntando al jar.

## Ejecución (sugerida)

Abrir el proyecto en IntelliJ IDEA y ejecutar cada módulo por separado:

1. Ejecutar `ServidorCentral` (clase principal: `servidor.ServidorCentral`).
2. Ejecutar `Pantalla` (clase principal: `pantalla.MonitorSala`).
3. Ejecutar `Operador` (clase principal: `operador.PanelOperador`).
4. Ejecutar `kiosko` (clase principal: `kiosco.Kiosco`).

Orden recomendado para pruebas locales: primero `ServidorCentral`, luego `Pantalla` y finalmente `Operador`/`Kiosco`.

## Comportamiento relevante implementado

- El `MonitorSala` carga al inicio los últimos llamados persistidos y muestra el historial con el más reciente arriba.
- Al llamar a un nuevo cliente, el turno anterior pasa al tope del historial empujando hacia abajo el resto.
- Los archivos de persistencia están cifrados; la lectura/escritura se realiza a través de las clases `*JSON`, `*XML` o `*TP` según la fábrica de persistencia.

## Notas para desarrollo

- Las rutas de archivos son relativas al directorio de trabajo al ejecutar las aplicaciones; si corres desde IDE, los archivos se crean/leen en la raíz del workspace.
- Para forzar que VS Code reconozca la dependencia de Gson, recargar la ventana o reiniciar el servidor Java puede ser necesario.

---
