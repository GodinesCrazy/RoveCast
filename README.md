# RoveCast - Android Radio Player

## Descripción General

RoveCast es una aplicación de radio por internet para Android que permite a los usuarios descubrir y escuchar emisoras de radio de todo el mundo. La aplicación utiliza la API de [Radio Browser](https://www.radio-browser.info/) para obtener la lista de emisoras.

## Características Principales

- **Descubrimiento de Emisoras:** Explora miles de emisoras de radio por país, idioma o género.
- **Favoritos:** Guarda tus emisoras favoritas para acceder a ellas rápidamente.
- **Búsqueda:** Encuentra emisoras por nombre o palabra clave.
- **Reproductor Moderno:** Una interfaz de reproductor limpia y fácil de usar.
- **Soporte para Chromecast:** Transmite tu música a cualquier dispositivo compatible con Google Cast.
- **Temporizador de Apagado (Sleep Timer):** Programa la aplicación para que se detenga después de un tiempo determinado.
- **Alarma:** Despiértate con tu emisora de radio favorita.

## Sistema de Monetización

La aplicación utiliza un modelo híbrido con anuncios y una compra única para eliminarlos.

### Publicidad (AdMob)

- **Banner:** Se muestra un banner en la parte inferior de la pantalla principal.
- **Intersticial:** Se muestra un anuncio a pantalla completa al cerrar la pantalla del reproductor, con un límite de frecuencia para no ser intrusivo.

### Versión Premium

- Los usuarios pueden realizar una compra única en la aplicación para eliminar todos los anuncios de forma permanente.

## Gestión de Consentimiento (UMP)

La aplicación integra la Plataforma de Mensajería de Usuario (UMP) de Google para gestionar el consentimiento de los usuarios en regiones que lo requieren (como el EEE y el Reino Unido), de acuerdo con el GDPR.

- El diálogo de consentimiento se muestra al iniciar la aplicación si es necesario.
- Los anuncios solo se cargan y muestran si se ha obtenido el consentimiento del usuario.
- Los usuarios pueden cambiar sus preferencias de consentimiento en cualquier momento desde la pantalla de "Política de Privacidad".

## Instrucciones de Compilación

1.  Clona este repositorio.
2.  Abre el proyecto en Android Studio.
3.  Asegúrate de tener el SDK de Android y las herramientas de compilación necesarias instaladas.
4.  Configura tu propio archivo `google-services.json` de Firebase si deseas utilizar los servicios de Google.
5.  Configura las credenciales de firma para la versión de lanzamiento en el archivo `local.properties` o en las variables de entorno.
6.  Construye el proyecto usando Gradle: `gradlew build`.

