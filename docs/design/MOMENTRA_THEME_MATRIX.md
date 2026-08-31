# Momentra Theme Matrix

**Authority for S2–S4 Moment/context colors.**  
**Date:** 2026-08-26  
**Figma root:** `TzLvwVwlPbeVB8ug1zB3GM` / `169:487`  
**Rule:** Components request `ContextTheme.contextAccent` or `MomentTheme.primary` explicitly — never a collapsed `currentColor`.  
**Life360** is `GlobalSurfaceTheme`, not a ContextTheme.

Figma MCP was unavailable during S1-B extraction. Values below are from Phase 4 mapping + committed native theme sources. Rows marked `FIGMA_GAP` must be re-verified against live Figma before S2 visual polish.

## ContextTheme

| Scope | Family/type | Primary (contextAccent) | Secondary | Icon | Figma node |
|-------|-------------|-------------------------|-----------|------|------------|
| Context | PERSONAL | `#7C5CFC` | `#A78BFA` | context tab | `763:12897` |
| Context | GROUP | `#E8621A` | `#FF8E63` | context tab | `763:12897` |
| Context | BUSINESS | `#818CF8` | `#A5B4FC` | context tab | `763:12897` |
| Context | CIRCLE | `#E86BA3` | `#FF6B8A` / selected `#FC6A8B` | context tab | **S6-F** `1075:7556` |

## GlobalSurfaceTheme

| Scope | Family/type | Primary | Secondary | Icon | Figma node |
|-------|-------------|---------|-----------|------|------------|
| GlobalSurface | LIFE360 | `#0C0F15` surface / `#1E293B` action | `#10B981` online | radar `ic_shell_radar` | TopBar `763:12896` |
| GlobalSurface | LIFE360 Coming Soon | `#14121B` page / `#161B26` card | `#F2CA50`→`#FFAB40` gold | decorative radar | **S5-H** `1075:7637` |

## Circle Coming Soon (context body)

| Scope | Family/type | Primary | Secondary | Icon | Figma node |
|-------|-------------|---------|-----------|------|------------|
| Context body | CIRCLE Coming Soon | `#14121B`→`#1C1B1B` / `#E86BA3` | `#FF6B8A` / lavender `#B794F6` | network illustration | **S6-F** `1075:7556` |

## Personal MomentTheme

| Scope | Family/type | Primary | Secondary | Icon | Figma node |
|-------|-------------|---------|-----------|------|------------|
| Personal | Life Operations | `#7C5CFC` | `#A78BFA` | pulse family | `353:8893` |
| Personal | Relationships | `#E91E63` | `#E12A9E` / `#A78BFA` | pulse family | `505:11793` |
| Personal | Future Building | `#10B981` | `#34D399` | pulse family `353:6905` + MomentThemes | **SCREEN_STALE resolved (S2-J):** Pulse family heroes aligned to matrix emerald; prior purple `#6C4EF2`/`#8B5CF6` removed from PersonalPulseFamily |
| Personal | Lifestyle | `#0EA5A4` | `#7C5CFC` | pulse family | `505:12365` |

## Group MomentTheme

| Scope | Family/type | Primary | Secondary | Icon | Figma node |
|-------|-------------|---------|-----------|------|------------|
| Group / Shared Experience | Trip | `#E8744F` | `#FF8E63` | setup type | Group setup `575:9917` |
| Group / Shared Experience | Wedding | `#EC4899` | `#F472B6` | setup type | `575:9917` |
| Group / Shared Experience | House Party | `#3B82F6` | `#60A5FA` | setup type | `575:9917` |
| Group / Shared Experience | Office Outing | `#14B8A6` | `#2DD4BF` | setup type | `575:9917` |
| Group / Shared Purchase | Gift Pool | `#E8621A` | `#FF8E63` | **FIGMA_GAP** | inherit Group accent until Figma type node |
| Group / Shared Purchase | Group Purchase | `#E8621A` | `#FF8E63` | **FIGMA_GAP** | inherit |
| Group / Shared Purchase | Shared Asset | `#E8621A` | `#A78BFA` | **FIGMA_GAP** | inherit |
| Group / Shared Purchase | Custom | `#E8621A` | `#C9C4D9` | **FIGMA_GAP** | inherit |
| Group / Shared Living | Flatmates | `#14B8A6` | `#2DD4BF` | **FIGMA_GAP** | living family seed |
| Group / Shared Living | Family Household | `#EC4899` | `#F472B6` | **FIGMA_GAP** | living family seed |
| Group / Shared Living | Co-living | `#3B82F6` | `#60A5FA` | **FIGMA_GAP** | living family seed |
| Group / Shared Living | Custom | `#E8621A` | `#C9C4D9` | **FIGMA_GAP** | inherit |

## Business MomentTheme

| Scope | Family/type | Primary | Secondary | Icon | Figma node |
|-------|-------------|---------|-----------|------|------------|
| Business | Team Operations | `#818CF8` | `#A5B4FC` | biz create | **FIGMA_GAP** — uses Business context accent until distinct Figma swatch |
| Business | Business Runway | `#34D399` | `#6EE7B7` | biz create | **FIGMA_GAP** — seed from runway memory green in create UI |
| Business | Business Operations | `#818CF8` | `#FB923C` | biz create | **FIGMA_GAP** |

## Global chrome surfaces

| Token | Value | Use |
|-------|-------|-----|
| TopBar / BottomBar bg | `#0C0F15` | GlobalTheme |
| Surface content | `#14121B` | GlobalTheme |
| Context unselected | `#C9C4D9` | ContextSwitcher |
| Bottom selected | `#C9BFFF` | BottomNav selected glyph |
| Bottom unselected | `#C9C4D8` | BottomNav |
| Company chip bg / border | `#1A2030` / `#3A4258` | CompanySwitcher |
| Action circle | `#1E293B` | TopBar Life360 |
| CTA orange | `#E8621A` | Global Create Moment control |

## Component color binding (mandatory)

| Component | Token |
|-----------|-------|
| ContextSwitcher | `ContextTheme.contextAccent` |
| MomentSwitcher | `MomentTheme.primary` |
| QuickAddLauncher | `MomentTheme.primary` |
| Moment cards | `MomentTheme` |
| TopBar | GlobalTheme + Figma rules (not Moment primary) |
| BottomNav selected | Context accent **or** Figma BottomSelected `#C9BFFF` per design |
| Life360 entry | `GlobalSurfaceTheme.life360` |
