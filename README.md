# audio-plug (KyronixStudio)

[![GitHub Release](https://img.shields.io/github/v/release/paradox6ygf/audio-plug)](https://github.com/paradox6ygf/audio-plug/releases/new)
![Stability](https://img.shields.io/badge/stability-production--ready-brightgreen)
![Lavalink](https://img.shields.io/badge/lavalink-v4+-orange)

A high-performance, resilient, and enterprise-grade plugin for Lavalink v4+. Developed by **KyronixStudio**, this plugin enables native streaming from **Gaana**, **Amazon Music**, and **Audiomack**.

---

### 📥 Latest Version & Downloads
The latest production-ready builds are always available here:  
**🚀 [Create or View New Releases](https://github.com/paradox6ygf/audio-plug/releases/new)**

#### Release Assets
- **[Download Latest Plugin (.jar)](https://github.com/paradox6ygf/audio-plug/releases/new)**
- [Source code (zip)](https://github.com/paradox6ygf/audio-plug/archive/refs/tags/v1.0.0.zip)

---

## 🚀 Native Platform Support

- **Gaana Integration**: Full access to the Gaana library through native API extraction.
- **Amazon Music High-Quality**: Native stream resolution with session persistence.
- **Audiomack Support**: Seamless playback for Audiomack tracks, albums, and playlists.

## 🛠️ Zero-Error Engineering
- **Resilient HTTP Pipeline**: Backed by OkHttp with smart retries and exponential backoff.
- **Atomic Exception Handling**: All failures are bridged safely to Lavalink to prevent server crashes.
- **Non-blocking I/O**: High-performance metadata and track resolution.

---

## 💻 Configuration (`application.yml`)

Configure the plugin properties within your Lavalink `application.yml`:

```yaml
lavalink:
  plugins:
    - dependency: "com.kyronixstudio:kyronixstudio:1.0.0"
      repository: "local"

plugins:
  kyronixstudio:
    sources:
      gaanaEnabled: true
      amazonEnabled: true
      audiomackEnabled: true
    http:
      timeout: 15000     # Time to wait (ms) before assuming failure
      retries: 3         # Retries on network instability
    cache:
      maxSize: 500       # Memory limit for track metadata
```

## 🔍 Search Engine Prefixes

| Service | Search Tag | Example |
| :--- | :--- | :--- |
| **Gaana** | `gaanasearch:` | `gaanasearch: Bollywood Hits` |
| **Amazon Music** | `amzsearch:` | `amzsearch: Shape of You` |
| **Audiomack** | `amacksearch:` | `amacksearch: Afrobeats` |
| **Spotify** | `spsearch:` | *Coming soon in next update* |
| **JioSaavn** | `jssaech:` | *Coming soon in next update* |

---

## ⚡ Developer & Releases
Maintained by **KyronixStudio**.  
Visit the [New Release Page](https://github.com/paradox6ygf/audio-plug/releases/new) to upload your custom builds or check for security patches.

Repo: [paradox6ygf/audio-plug](https://github.com/paradox6ygf/audio-plug.git)
