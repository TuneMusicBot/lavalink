# TuneMusicBot/lavalink

A custom lavalink forked from [davidffa](https://github.com/davidffa/lavalink).

## Changes
- Converted all  stuff to kotlin
- Refactorings
- Remove useless files
- Dependency updates
- Use [my custom lavaplayer-fork](https://github.com/WearifulCupid0/lavaplayer) forked from [Walkyst's fork](https://github.com/walkyst/lavaplayer-fork).
- Add a lot of audio source managers.
- Added WebSocket op code "ping" (responds with `{ "op": "pong" }`, useful to check WS latency between lavalink node and the bot). If you send the guildId property, lavalink responds with `{ "op": "pong", "ping": x }` where `x` is the latency between discord voice gateway and the lavalink node.
- Added dependencies versions in the headers of every request.
- Support audio receiving (see how to use it [here](https://github.com/davidffa/lavalink/pull/2))
- Increase amount of data that PlayerUpdate event send.
- Custom parsing of errors forked from [natanbc andesite](https://github.com/natanbc/andesite).
- Format XM Support.

## Credits

- [Freya Arbjerg](https://github.com/freyacodes) (For original lavalink)
- [melike2d](https://github.com/melike2d) and [davidffa](https://github.com/davidffa) (For custom lavalink)
- [sedmelluq](https://github.com/sedmelluq) (For original lavaplayer)
- [Walkyst](https://github.com/walkyst) (For lavaplayer-fork)
- [natanbc](https://github.com/natanbc) (For lavadsp audio filters)