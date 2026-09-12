# Momentra invite landing (momentra.tech)

Static hosting for group/company invite HTTPS links and App / Universal Link verification.

## Production host

**Canonical domain:** `https://momentra.tech` (Vercel)

- Group: `https://momentra.tech/j/{code}`
- Company: `https://momentra.tech/c/{code}`
- Well-known: `https://momentra.tech/.well-known/assetlinks.json` and `apple-app-site-association`

Clients mint `https://momentra.tech/j|c/…`. Backend uses `PUBLIC_APP_ORIGIN` / `INVITE_DISPLAY_ORIGIN` (default `https://momentra.tech`).

Legacy hosts (`momentra.app`, `momentra-v2.web.app`) still parse in-app.

## Deploy (Vercel)

Point the **momentra.tech** Vercel project root (or a rewrite) at this folder (`web/invite`), then:

```bash
# from repo root, if this folder is its own Vercel project:
npx vercel --prod --cwd web/invite
```

Or merge [`vercel.json`](vercel.json) rewrites/headers into the existing marketing site so `/j/*`, `/c/*`, and `/.well-known/*` are served from these files (override older `app.momentra` / `MagnatePoint.Momentra` well-known entries with the v2 files in `.well-known/`).

## Paths

| Path | Purpose |
|------|---------|
| `/j/{code}` / `/join/{code}` | Group invite landing → opens `momentra://j/{code}` |
| `/c/{code}` / `/company/{code}` | Company invite landing → opens `momentra://c/{code}` |
| `/.well-known/assetlinks.json` | Android App Links (`com.example.momentra`) |
| `/.well-known/apple-app-site-association` | iOS Universal Links (`resolvingpoint.momentra`) |

## Android fingerprint

`assetlinks.json` includes the **debug** keystore SHA-256. For Play / release / App Distribution builds, append the upload/app-signing certificate fingerprint (no colons):

```bash
keytool -list -v -keystore your-release.keystore -alias your-alias
```

## iOS

Associated domains: `applinks:momentra.tech` (and www). Enable Associated Domains for App ID `resolvingpoint.momentra` in the Apple Developer portal.
