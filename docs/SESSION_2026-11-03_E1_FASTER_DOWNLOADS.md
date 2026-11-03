# SESSION 2026-11-03 E1: faster downloads

Implemented and pushed E1 to main.

- Increased the process download semaphore from one to two concurrent transfers.
- The manager publishes an indeterminate `Connecting...` state as soon as a download is accepted; the PDF reader and hub row show that state before bytes arrive.
- The C1 `size_bytes` value is passed from the material list into the manager / reader and seeds the cached total.
- Book transfers update progress on the first received chunk, then on each percentage change.
- Connect timeout is 15 seconds and read timeout remains 60 seconds. Transient I/O errors and HTTP 5xx / 408 / 429 responses retry up to three attempts with increasing waits. Partial `.part` data is retained and resumed with Range.

No APK build was run by the agent. Test two large downloads together on a slow connection, verify `Connecting...` appears immediately, then interrupt one and confirm the retry/resume behavior.

Next: B1 — taller material cards across the hub lists.
