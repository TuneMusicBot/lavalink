# davidffa/lavalink

A custom lavalink forked from [melike2d](https://github.com/melike2d/lavalink).

## Changes
- Converted all  stuff to kotlin
- Refactorings
- Remove useless files
- Dependency updates
- Use [my custom lavaplayer-fork](https://github.com/davidffa/lavaplayer-fork/tree/custom) forked from [Walkyst](https://github.com/walkyst/lavaplayer-fork).
- Add Getyarn, Reddit, Yandex Music, Odysee sources. Yandex & Odysee search (`ymsearch:` `odsearch:` prefixes).
- Added WebSocket op code "ping" (responds with `{ "op": "pong" }`, useful to check WS ping).
- Added GET /versions route, returns info about jvm version, kotlin version, spring version, build time, etc.

## Credits

- [melike2d](https://github.com/melike2d) (For custom lavalink)
- [Walkyst](https://github.com/walkyst) (For lavaplayer-fork)
- [sedmelluq](https://github.com/sedmelluq) (For original lavaplayer)
- [Freya Arbjerg](https://github.com/freyacodes) (For original lavalink)
- [natanbc](https://github.com/natanbc) (For lavadsp filters)
