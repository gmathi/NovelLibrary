## 🎉 v2.4.0

Offline reading that stays put, a tap-to-open reader, and reliable RoyalRoad loading.

- 📶 **Downloaded chapters stay visible offline** — Opening a novel with no connection no longer hides your list. Downloaded chapters are served straight from the device, and the app falls back to them if a refresh can't reach the network — so your offline library is always there.
- 👆 **Tap the screen to open the reader menu** — Tap anywhere in the reading area to toggle the reader menu — no more hunting for the floating button (now hidden). Taps on links, images and text selection keep working as before, and the system bars reveal alongside the reader bars.
- 🛡️ **RoyalRoad loads reliably again** — RoyalRoad's new JavaScript anti-bot check broke chapter loading. RoyalRoad now always routes through the in-app browser fetcher, which runs the verification like a real browser, so chapters load again.
- 📱 **App icon resumes where you left off** — Tapping the app icon now returns to whatever you had open (such as the reader) instead of dropping you back on the main screen.
- 🔊 **Cleaner TTS notification behaviour** — Tapping the text-to-speech notification no longer stacks duplicate reader and controls screens — it reuses the one already open.
- 🗓️ **Accurate "Last Updated" after a refresh** — Refreshing a single novel now updates its Last Updated date, matching the behaviour of a full category sync.
- 🐛 **Fewer false error reports** — Cancelling a fetch (for example during Cloudflare verification or a screen rotation) no longer shows a spurious error prompting you to report a non-bug.
- 📜 **Chapter-list scrollbar restored** — The vertical scrollbar is back in the chapter list.
