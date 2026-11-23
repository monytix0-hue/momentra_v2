# Business V4 P4 — Server Quick Add inventory

Supersedes: thin “catalog codes + amount/title form only” gap note in
`docs/qa/BUSINESS_V4_P4_MOBILE_GATE.md` P4.3 / unresolved gap #2.
Linked from: `BUSINESS_V4_P4_MOBILE_GATE.md`, `docs/qa/business-v4-p4-maestro/README.md`.
User instruction: "Inventory every server Quick Add and its required request fields."

Source of truth: `backend/typescript/src/modules/business/operating-catalog.ts`.
Clients: `TypedOperatingQuickAdd.kt` / `TypedOperatingQuickAdd.swift`.

## Canonical POST

`POST /business/moments/{momentId}/operating-events` + `Idempotency-Key`

Shared request fields (server schema): `eventType`, `title`, optional `amount`, `note`,
`channelName`, `counterpartyName`, `paymentStatus`, `parentEventId`, `quantity`, `unit`,
`dueAt`, `lineItems[]`, `contract{}`, marketing/metric fields. Server validates and totals;
clients never compute authoritative finance.

## Per-subtype catalogs

| Subtype | Quick Add codes (server order) |
| ------- | ------------------------------ |
| TRADING_WHOLESALE | SALE, PURCHASE, SPEND, PAYMENT, COLLECTION, BUDGET, UPDATE, ISSUE, APPROVAL, STOCK, ORDER, SHIPMENT, RETURN |
| DISTRIBUTION_DEALERSHIP | + DELIVERY (same as trading otherwise) |
| CLIENT_CAMPAIGN | ACTIVITY, PROGRESS, SPEND, PAYMENT, REVISION, APPROVAL, UPDATE, RESULT, INVOICE, COLLECTION |
| OWN_BUSINESS_CAMPAIGN | Same as Client **without** INVOICE, COLLECTION (client filters if leaked) |
| RETAIL_STORE | SALE, PURCHASE, SPEND, PAYMENT, COLLECTION, BUDGET, TARGET, UPDATE, ISSUE, APPROVAL, STOCK, ORDER |
| RESTAURANT_FOOD | Same as Retail (+ table/covers on SALE) |
| CONSUMER_SERVICE | SERVICE, APPOINTMENT, PURCHASE, SPEND, PAYMENT, COLLECTION, BUDGET, TARGET, UPDATE, ISSUE, APPROVAL, STOCK |
| STANDARD_PRODUCTION | PRODUCTION, PURCHASE, SPEND, SALE, CUSTOMER_ORDER, DELIVERY, PAYMENT, COLLECTION, BUDGET, TARGET, UPDATE, ISSUE, APPROVAL, STOCK |
| MADE_TO_ORDER | Same as Standard (+ optional parent on PRODUCTION/SALE) |

## Typed required fields (client `quickAddFieldSpec`)

| eventType | Required | Notable optional / contract |
| --------- | -------- | --------------------------- |
| SALE / SERVICE | title, amount (+ channel retailish) | lines, GST taxMode/gstRate, party, payment, table/covers (restaurant), parent (MTO) |
| INVOICE | title, amount, party | dueAt, GST |
| PURCHASE | title, amount, party | lines, GST, payment |
| SPEND | title, amount, expenseCategory | costBasis (Client Campaign) |
| PAYMENT / COLLECTION | title, amount, parent | eligible-source picker |
| STOCK | title, item, qty, stockAction | location, reason |
| ORDER / CUSTOMER_ORDER | title, party (+ channel retailish) | amount, lines |
| SHIPMENT / DELIVERY | title, qty, **parent** | |
| RETURN | title, amount, returnReason | parent, GST, lines |
| PRODUCTION | title, qty | rejectedQty, parent (MTO/BOM job link) |
| APPOINTMENT | title | party |
| BUDGET / TARGET | title, amount | metricCode |
| REVISION | title, revisionKind | amount, metricCode |
| PROGRESS | title, parent | activityStatus |
| RESULT | title | attributedSales, period, metric |
| ACTIVITY / UPDATE / ISSUE / APPROVAL | title | note, amount |

## Parent eligibility (server)

- PAYMENT → PURCHASE, SPEND, VENDOR_COST
- COLLECTION → SALE, SERVICE, CUSTOMER_ORDER, INVOICE
- SHIPMENT / DELIVERY → ORDER, CUSTOMER_ORDER
- Optional: PRODUCTION → CUSTOMER_ORDER, TARGET; SALE → CUSTOMER_ORDER, ORDER, APPOINTMENT

Clients prefer `paymentReadiness` + activity rows as pickers; preserve draft on 409/stale-parent.
