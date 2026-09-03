# Momentra invite landing (momentra.app)

Static hosting for group/company invite HTTPS links and App / Universal Link verification.

## Live deploy

Firebase Hosting site **momentra-v2** is deployed:

- https://momentra-v2.web.app/j/{code}
- https://momentra-v2.web.app/.well-known/assetlinks.json
- https://momentra-v2.web.app/.well-known/apple-app-site-association

**Cutover for `momentra.app`:** Firebase custom domain `momentra.app` is registered on site **momentra-v2** but DNS still points at Cloudflare→GitHub Pages (`HOST_MISMATCH`). In Cloudflare DNS for `momentra.app`, apply Firebase’s desired records:

- **A** `momentra.app` → `199.36.158.100` (remove Cloudflare proxy A/AAAA to 104.21… / 172.67…)
- **TXT** `momentra.app` → `hosting-site=momentra-v2`
- **TXT** `_acme-challenge.momentra.app` → (value shown in Firebase Console while cert validates)

Until DNS flips, invite HTTPS + well-known files are live at https://momentra-v2.web.app. Clients already mint `https://momentra.app/j/…`; App Links / Universal Links verify once that host serves `assetlinks.json` / AASA.



## Paths

| Path | Purpose |
|------|---------|
| `/j/{code}` / `/join/{code}` | Group invite landing → opens `momentra://j/{code}` |
| `/c/{code}` / `/company/{code}` | Company invite landing → opens `momentra://c/{code}` |
| `/.well-known/assetlinks.json` | Android App Links |
| `/.well-known/apple-app-site-association` | iOS Universal Links |

## Deploy (Firebase Hosting)

```bash
npx -y firebase-tools@latest deploy --only hosting --project momentra-v2
```

## Android fingerprint

`assetlinks.json` includes the **debug** keystore SHA-256. For Play / release builds, append the upload/app-signing certificate fingerprint:

```bash
keytool -list -v -keystore your-release.keystore -alias your-alias
```

Use the SHA-256 value **without colons**.

## iOS

Associated domains are set in `momentra.entitlements` (`applinks:momentra.app`). Enable Associated Domains for App ID `resolvingpoint.momentra` in the Apple Developer portal if not already.
