# Android Trading and Distribution Code Structure Exploration

## Summary

The Android codebase is located under `/Volumes/coding/momentra_v2/apk/app/src/main/java/com/example/momentra/`. The Trading and Distribution screens are part of the operating moments system under `ui/shell/business/`.

## Key Files Found

### Trading
- **Screen**: `/Volumes/coding/momentra_v2/apk/app/src/main/java/com/example/momentra/ui/shell/business/trading/TradingActive.kt`
  - Main Trading active screen with Pulse, Moments, Life, Memory tabs
  - Uses `OperatingProjectionDto` model
  - Color scheme: TradeBg (0xFF09121F), TradeCard (0xFF122235), TradeBrand (0xFFF59E0B)
  - Features: Pulse view with sales target/budget, Attention, Payment Readiness, Finance, GST Position
  - Activities: Recent activity list with view all sheet
  - Quick Add hub integration

- **Quick Adds**: `/Volumes/coding/momentra_v2/apk/app/src/main/java/com/example/momentra/ui/shell/business/trading/TradingQuickAdds.kt`
  - `TradingEventDraft` data class with 26 fields (eventType, title, amount, note, channelName, counterpartyName, paymentStatus, occurredAt, quantity, unit, unitPrice, itemName, parentEventId, revisesEventId, masterKind, conversionToBase, dueAt, contract, masterId, masterGst fields, lines)
  - `TradingSheet` enum with 17 action types: SALE, PURCHASE, SPEND, PAYMENT, COLLECTION, BUDGET, UPDATE, ISSUE, STOCK, ORDER, SHIPMENT, RETURN, REIMBURSE, PERSON, ROLE, INVITE, SUPPLIER
  - Quick Add Hub with search, core actions grid, masters, support & management sections
  - Role-based visibility (Owner, Approver)

### Distribution
- **Screen**: `/Volumes/coding/momentra_v2/apk/app/src/main/java/com/example/momentra/ui/shell/business/distribution/DistributionActive.kt`
  - Main Distribution active screen with Pulse, Moments, Life, Memory tabs
  - Mirrors Trading structure with Distribution-specific content
  - Color scheme: DistBg (0xFF071014), DistCard (0xFF0C171B), DistBrand (0xFFFBBF24)
  - Features: Sales plan, distribution commitments, dispatch tracking

- **Quick Adds**: `/Volumes/coding/momentra_v2/apk/app/src/main/java/com/example/momentra/ui/shell/business/distribution/DistributionQuickAdds.kt`
  - `DistributionEventDraft` data class (same structure as Trading)
  - `DistributionSheet` enum with 16 action types (similar to Trading but with Delivery Update instead of Shipment)
  - Same Quick Add Hub pattern

## Core Architecture

### Operating Moments System
- Entry point: `OperatingMomentScreens.kt`
- Maps moment subtypes to screens:
  - `TRADING_WHOLESALE` → `TradingActive`
  - `DISTRIBUTION_DEALERSHIP` → `DistributionActive`
  - Uses `BusinessSliceRepository` for API calls

### Shared Components
- **Repository**: `/Volumes/coding/momentra_v2/apk/app/src/main/java/com/example/momentra/data/repository/BusinessSliceRepository.kt`
  - `getOperating(momentId)` - fetches OperatingProjectionDto
  - `recordOperatingEvent()` - records events with full parameters
  - `decideOperatingEvent()` - handles approvals

- **Data Models**:
  - `OperatingProjectionDto` - main data model with:
    - finance, budget, target, attention, paymentReadiness
    - activity list, boards (moments/life/memory)
    - quickAdds list, gstPosition
    - masters list

- **Capture System**:
  - `OperatingCaptureDraft` - base draft model
  - `OperatingQuickSheet.kt` - reusable capture sheet with variant handling
  - Supports stock management, sales recording, purchases, payments, etc.

### Stock Management
Stock features in OperatingQuickSheet:
- StockCapture for trading/distribution:
  - For contract family with lovTrade: Item selection via LOV, Action (Set/Add/Reduce), Quantity, Unit, Reason, Ownership
  - For non-contract: Availability status dropdown (Available/Needs check/Low availability for distribution, Available/Needs check/Low availability for trading)
  - Stock updates create operational signals, no purchase/sale record

### Sales Recording
- Sale capture mode in OperatingQuickSheet
- Supports line items with quantity, unit, unit price
- GST tracking for trading/distribution
- Channel selection
- Contract body support

## API Integration
- Backend API: `ApiService.getOperatingMoment(momentId)` returns `SuccessEnvelope<OperatingProjectionDto>`
- Record events via `api.recordOperatingEvent(momentId, idempotencyKey, body)`
- Uses idempotency keys for all writes

## Code Patterns

### Screen Structure
Both Trading and Distribution follow identical patterns:
1. `TradingActive`/`DistributionActive` Composable with tab navigation
2. Sub-composables for each tab (Pulse, Moments, Life, Memory)
3. QuickAddHub for recording events
4. Activity sheet for viewing all activity
5. Role-based visibility for finance data

### Color Schemes
Each industry type has distinct branding colors defined as internal vals.

### Event Types Supported
- SALE, PURCHASE, SPEND, PAYMENT, COLLECTION
- BUDGET, UPDATE, ISSUE
- STOCK, ORDER, SHIPMENT, RETURN
- REIMBURSE, PERSON, ROLE, INVITE, SUPPLIER

## Backend Relation
- TypeScript backend modules at `/Volumes/coding/momentra_v2/backend/typescript/src/modules/business/`
- Trading contracts and LOVs exist but may be legacy
- Current Android uses unified operating projection system
