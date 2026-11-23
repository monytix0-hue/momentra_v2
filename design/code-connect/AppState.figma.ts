// url=<MOMENTRA>?node-id=2666-32397
// source=momentra/momentra/Shell/BusinessAppState/BusinessAppStateContent.swift
// component=BusinessAppStateBody
import figma from 'figma'

const instance = figma.selectedInstance

// Figma component set `AppState` (2666:32397) exposes exactly two VARIANT
// properties. Every option below exists in the set's
// `componentPropertyDefinitions`; none are invented.
const screen = instance.getEnum('Screen', {
  Pulse: 'pulse',
  Moments: 'moments',
  Life: 'life',
  Memory: 'memory',
  Hub: 'hub',
  ViewAll: 'viewAll',
  NoCompany: 'noCompany',
  Offline: 'offline',
})

const phase = instance.getEnum('State', {
  Empty: 'empty',
  Loading: 'loading',
  Error: 'error',
  Setup: 'setup',
})

// `BusinessAppStateAccent` has a single case — the kit collapsed every vertical
// onto emerald (#10B981), so there is nothing to read off the instance.
const accent = 'emerald'

// Figma's four loading frames each carry one body text layer, `Loading <Screen>…`
// (2666:29681, 2666:30225, 2666:30769, 2666:31313). Hub, ViewAll, NoCompany and
// Offline have no Loading variant — see the gap list below.
const loadingLabels: Record<string, string | null> = {
  pulse: 'Loading Pulse…',
  moments: 'Loading Moments…',
  life: 'Loading Life…',
  memory: 'Loading Memory…',
  hub: null,
  viewAll: null,
  noCompany: null,
  offline: null,
}

// Variant gaps in 2666:32397 (17 of the 32 Screen x State combinations exist,
// so 15 are absent). Every option above is still mapped, because `.figma.ts`
// must handle any instance the designer drags out of the set:
//   Setup   — absent for Pulse, Moments, Life, Memory, Hub, ViewAll, Offline
//   Loading — absent for Hub, ViewAll, NoCompany, Offline
//   Error   — absent for ViewAll, NoCompany
//   Empty   — absent for NoCompany, Offline
// `spec(_:_:)` still resolves those combinations at runtime (Pulse/Moments/Life/
// Memory `.setup` collapse onto their `.empty` copy; Hub `.loading`/`.setup`
// collapse onto `.empty`; ViewAll `.loading`/`.error` collapse onto `.empty`),
// so the gaps are Figma coverage, not code drift.
const loadingLabel = loadingLabels[screen]

let snippet

if (phase === 'loading' && loadingLabel) {
  // BusinessAppStateLoadingBody(label:accent:) — BusinessAppStateContent.swift:124
  snippet = figma.swift`BusinessAppStateLoadingBody(
    label: "${loadingLabel}",
    accent: .${accent}
)`
} else if (screen === 'hub' && phase === 'empty') {
  // BusinessAppStateHubBody(accent:vertical:onPrimary:onSecondary:) —
  // BusinessAppStateContent.swift:171. Figma draws the unmodified tile set
  // ("Stock"), which is what `hubTileLabels(vertical:)` returns for every
  // vertical except `.services` and `.marketing`; `.trading` is that default.
  snippet = figma.swift`BusinessAppStateHubBody(
    accent: .${accent},
    vertical: .trading,
    onPrimary: {},
    onSecondary: {}
)`
} else if (screen === 'viewAll') {
  // ViewAll is the only variant that ships as a sheet, not a body
  // (BusinessAppStateContent.swift:288). `selectedFilter` is a caller-owned
  // Binding<String>; `.constant` keeps the snippet self-contained.
  snippet = figma.swift`// Presented as .sheet — replace .constant with the caller's @State binding.
BusinessAppStateViewAllSheet(
    accent: .${accent},
    selectedFilter: .constant("All"),
    onClose: {},
    onShowAll: {}
)`
} else {
  // BusinessAppStateBody(spec:accent:onPrimary:onSecondary:) —
  // BusinessAppStateContent.swift:19. `BusinessAppStateSpecs.spec(_:_:)` takes
  // (screen, phase) and resolves the copy from BusinessAppStateSpecs.swift.
  snippet = figma.swift`BusinessAppStateBody(
    spec: BusinessAppStateSpecs.spec(.${screen}, .${phase}),
    accent: .${accent},
    onPrimary: {},
    onSecondary: {}
)`
}

export default {
  example: snippet,
  imports: ['import SwiftUI'],
  id: 'business-app-state-body',
  metadata: {
    nestable: false,
    props: {
      screen,
      phase,
    },
  },
}
