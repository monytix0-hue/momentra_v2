# Code Connect (Figma → code)

Templates in this folder map Figma components to the SwiftUI views that actually
render them, so an agent or designer reading a Figma link gets
`BusinessAppStateBody(spec: BusinessAppStateSpecs.spec(.pulse, .empty), …)` back
instead of re-rolling the layout by hand.

Parserless templates only: `.figma.ts` files whose default export is a
`` figma.swift`…` `` tagged template. Never `.figma.tsx`, never `figma.connect()`.

| File | Figma | Code |
|------|-------|------|
| `AppState.figma.ts` | Legacy `AppState` set `2666:32397` (TzLvw; kit docs) | `BusinessAppStateBody` · `BusinessAppStateLoadingBody` · `BusinessAppStateHubBody` · `BusinessAppStateViewAllSheet` |
| `Continue.figma.ts` | Published `Continue` / `Onboarding / Navigation` `2:32` (rknf) | `BusinessContinueButton` on guided create |

`figma.config.json` at the repo root points at this folder, sets
`parser`/`language` to `swift`, and substitutes `<MOMENTRA>` for
`https://www.figma.com/design/rknfN9EikSCdqmqXogR29o/momentra`. Both `include`
(the key the CLI reads) and `includes` (the key we were asked for) are present
so either spelling resolves the same glob.

## Before publishing

1. Add the runtime types so editors stop flagging `import figma from 'figma'`:

   ```jsonc
   // tsconfig.json
   { "compilerOptions": { "types": ["@figma/code-connect/figma-types"] } }
   ```

   Until that install exists, `import 'figma'` is unresolved. That is the only
   `tsc` diagnostic the template produces. Two checks were run locally without
   network access:

   ```bash
   # syntax + bundle shape
   backend/typescript/node_modules/.bin/esbuild \
     design/code-connect/AppState.figma.ts --format=esm --target=es2022

   # full type-check; --module is resolved through an ambient shim because the
   # real package is not installed:
   #   declare module 'figma' {
   #     const figma: {
   #       selectedInstance: { getEnum(prop: string, map: Record<string, string>): string }
   #       swift: (s: TemplateStringsArray, ...v: unknown[]) => string
   #     }
   #     export default figma
   #   }
   backend/typescript/node_modules/.bin/tsc --noEmit --strict \
     --target es2022 --module esnext --moduleResolution bundler \
     --skipLibCheck figma-shim.d.ts design/code-connect/AppState.figma.ts
   ```

   Both exit 0. The shim declares only `figma.selectedInstance.getEnum` and the
   `figma.swift` tagged template — no Figma package was installed, and nothing
   was published.

2. Publish:

   ```bash
   npx @figma/code-connect@latest publish
   ```

   Run it from the repo root so it picks up `figma.config.json`. It needs a
   Figma personal access token (`FIGMA_ACCESS_TOKEN`).

**Nothing was published.** Publishing requires an Organization/Enterprise Figma
plan *and* the `AppState` component set to be published to a team library; the
file in this repo's export (`TzLvwVwlPbeVB8ug1zB3GM`) is not verified as
published. Until both hold, these templates are documentation, not live Dev Mode
snippets.

## Variant coverage

17 of the 32 `Screen × State` combinations exist in `2666:32397`. Missing:
`Setup` for Pulse/Moments/Life/Memory/Hub/ViewAll/Offline, `Loading` for
Hub/ViewAll/NoCompany/Offline, `Error` for ViewAll/NoCompany, `Empty` for
NoCompany/Offline. The Swift runtime collapses those (`BusinessAppStateSpecs.spec(_:_:)`
folds `.setup` onto the empty copy for Pulse/Moments/Life/Memory, Hub
`.loading`/`.setup` onto `.empty`, and ViewAll `.loading`/`.error` onto `.empty`), so
the gaps are Figma coverage, not code drift. See
[`docs/design/FIGMA_APPSTATE_DRIFT.md`](../../docs/design/FIGMA_APPSTATE_DRIFT.md).

## Regenerating the variant map

```bash
python3 scripts/figma/dump-appstate-variants.py            # markdown
python3 scripts/figma/dump-appstate-variants.py --json     # JSON
python3 scripts/figma/dump-appstate-variants.py --set MomentSwitcher
```

Reads `frame-2348-29188.json` from the repo root. That file is ~88 MB — load it
with `json`, never as text.

## Obsidian Emerald (rknf) note

V4 SoT file key is `rknfN9EikSCdqmqXogR29o`. Obsidian AppState frames under section `2:67`
are mostly **frames**, not a published component set — only **Continue** (`2:32`) is
Code-Connectable from that selection until AppState is republished in the rknf library.
`AppState.figma.ts` remains documentation for the kit against the legacy TzLvw set.

**Do not publish from the agent** unless explicitly asked. Publishing needs a Figma
personal access token (`FIGMA_ACCESS_TOKEN`) and an Organization/Enterprise plan:

```bash
npx @figma/code-connect@latest publish
```
