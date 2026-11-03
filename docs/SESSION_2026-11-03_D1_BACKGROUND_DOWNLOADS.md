# SESSION 2026-11-03 D1: app-level download manager

Implemented and pushed D1 to main.

- Added `OfflineDownloadManager`, a process-lifetime coordinator with a SupervisorJob/IO scope.
- Hub downloads now run in the manager, not the screen coroutine. Progress, failure, and completion are observable across screens; completion is saved in the existing downloaded-items preference.
- PDF reader downloads join the same manager-owned transfer, so leaving the reader no longer cancels the download.
- Book downloads use `<key>.part`, resume from the current byte count with HTTP Range, and restart safely if the server ignores Range. The final file is checked for a PDF header before it is renamed.
- Manager-owned URLs are protected from `CachingInputStream.close()` deleting a partial file or clearing progress.
- Transfers are serialized in this slice. E1 will add two concurrent downloads, immediate connecting state, and retries. Force-close survival is not promised.

No APK build was run by the agent. Test a large book download, navigate away and return, then interrupt the connection and re-tap to resume. Repeat from the PDF reader.

Next: E1 — faster start, two concurrent downloads, and retries.
