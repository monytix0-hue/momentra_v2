# OpenAPI client (not part of the app target)

Generated Swift sources live here so they are **not** auto-compiled by the Xcode
synced `momentra/` folder. The app currently uses `momentra/API/APIClient.swift`.

When you are ready to wire the generated client:

1. Add this directory as a local Swift package (restore `Package.swift`), or
2. Copy/link selected sources back into the app target and add the
   [AnyCodable](https://github.com/Flight-School/AnyCodable) package dependency.

Do not place a `Package.swift` (or these sources) under `momentra/momentra/` —
Xcode will compile them into the app module and collide with SwiftUI types
(e.g. `Configuration`).
