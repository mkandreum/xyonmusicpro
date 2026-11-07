<div align="center">
  <h1>XyonMusic</h1>
  <p><strong>Reproductor de música Android con barras de cristal líquido nativas, biblioteca local, streaming y Spotify.</strong></p>
  <p>
    <a href="https://github.com/mkandreum/xyonmusicpro/releases/latest">
      <img src="https://img.shields.io/badge/Descargar-APK-5268D8?style=for-the-badge&logo=android&logoColor=white" alt="Descargar XyonMusic">
    </a>
  </p>
</div>

## Qué es

XyonMusic es un reproductor Android (Kotlin, Jetpack Compose y Media3) con:

- Barras inferiores de **cristal líquido nativo** (`LiquidBar`: lente arrastrable, refracción AGSL,
  muelles, plegado al hacer scroll) compartidas por navegación y mini reproductor.
- Biblioteca local (SAF), streaming de YouTube Music, letras sincronizadas y ecualizador.
- Importación de playlists de Spotify con emparejamiento automático en YouTube.
- CoverFlow horizontal de los álbumes importados al girar el móvil.
- Tema claro/oscuro, controles estilo iOS y pantallas adaptables.

## Compilar

```bash
# Requiere JDK 21 y Android SDK 37
./gradlew assembleArm64FossRelease
```

El APK queda en `app/build/outputs/apk/arm64Foss/release/`.

## Créditos

XyonMusic es un fork rebautizado de [Convx](https://github.com/cosmictaserdev-creator/Convx),
publicado bajo GPL-3.0. Conserva las licencias y avisos de sus dependencias; ver
[LICENSE](LICENSE) y [NOTICE](NOTICE).

Las barras de cristal líquido son de [mkandreum/liquidbar](https://github.com/mkandreum/liquidbar).
