# TRD Figma Frame Map

Parent: `Trading · Northstar` (`5:12650`) in file `rknfN9EikSCdqmqXogR29o`.
Inventory row labels: `232:22262`–`232:22295`.

| Row | Section | Primary frames | Code path (Android / iOS) |
|---|---|---|---|
| 232:22262 | Main tabs | `5:15203` Pulse, `5:15516` Moments, `5:15807` Life, `5:16107` Memory, `5:16417` Hub | `TradingActive`+`OperatingV12Screens` / `OperatingMomentHubView`+`OperatingV12Screens` |
| 232:22263 | Setup | `5:18833` TRD-SETUP-v6 | `BusinessSetupShell` / `BusinessSetupShell` |
| 232:22264 | Masters | `5:17712` Invite, `5:17796` Principals, `5:17856` Catalogue, `5:17938` Warehouses | Masters host (new) |
| 232:22265 | Master forms | `5:17995` Principal, `5:18086` Warehouse, `5:18165` SKU, `5:18290` Price list | Masters forms (new) |
| 232:22266 | QA Sale | `5:17322` default, `5:22968` itemized, `44:16175` SO-88, `44:15957` received, `33:16306` validation | `TradingQuickAdds` SALE |
| 232:22267 | QA Purchase | `5:17402`, `5:23113` itemized, `44:16335` PO-2204 | PURCHASE |
| 232:22268 | QA Collection | `5:17473`, `39:15741` empty AR | COLLECTION |
| 232:22269 | QA Payment | `5:17527`, `44:15726` empty | PAYMENT |
| 232:22270 | QA Expense | `5:17580`, `5:23228` detailed | SPEND |
| 232:22271 | QA Stock | `5:17643` | STOCK |
| 232:22272 | QA Return | `5:16966`, `5:23311` supplier, `44:15784` empty | RETURN |
| 232:22273 | QA Budget/Target | `5:17035` | BUDGET / TARGET |
| 232:22274 | QA Update/Issue | `5:17089`, `5:23380` update | UPDATE / ISSUE |
| 232:22275 | QA Approval | `5:17142`, `44:16056` reject, `44:16112` reimburse | APPROVAL |
| 232:22276 | QA Order | `5:17198`, `5:23424` supplier | ORDER |
| 232:22277 | QA Delivery | `5:17261`, `5:23487` incoming, `44:15856` empty | DELIVERY / SHIPMENT |
| 232:22278 | QA utilities | `5:18410` hold, `5:18524` transfer, `5:18614` reprint | utilities |
| 232:22279 | Save/discard | `33:15708` saved, `33:16296` discard | toast / discard |
| 232:22280 | View all Pulse | `5:19827` Target, `5:19925` Finance, `5:20066` Payment, `5:20321` Attention, `5:20479` Activity, `5:20637` GST | `TradingSheetState` Pulse |
| 232:22281 | View all Moments | `5:20792` Orders, `5:20977` Stock, `5:21164` Network, `5:21322` Commitments | Moments sheets |
| 232:22282 | View all Life | `5:21423` Current, `5:21505` Dealers, `5:21609` Money, `5:21702` People | Life sheets |
| 232:22283 | View all Memory | `5:21801` Monthly, `5:21985` Fulfilment, `5:22063` People, `5:21893` Money | Memory sheets |
| 232:22284 | Stories P1–P8 | `5:20318` logos, `5:19757` config, `5:19892` composing, `5:19386`–`5:21249` P1–P8, `5:20012` saved, `5:20229` share | Story v3 viewer |
| 232:22285 | Story states | `5:22560`–`5:22889` empty/fail/conflict/unauth/partial/loading | Story state UI |
| 232:22286 | Moment switcher | `5:22963`–`5:22967` | MomentSwitcher |
| 232:22287 | App states tabs | `5:22946`–`5:22957` | App state shells |
| 232:22288 | App states hub | `5:22958`–`5:22962` | Hub/offline |
| 232:22289 | Utility sheets | `120:20957` GST, `120:21012` scan, `5:18704` SKU, `5:18764` batch, `36:15790` orders, `44:16481` search, `5:19696` switch | utilities |
| 232:22290 | Pickers dates | `5:19569` Month, `5:19445` Date, `5:19626` Date&time | date pickers |
| 232:22291 | Pickers people | `118:20030` Customer, `43:16435` Payee, `118:20098` Staff, `43:16110` Channel, `118:20166` Terms, `43:16045` Pay method | people pickers |
| 232:22292 | Pickers docs | `43:15726` invoices, `43:15790` payables, `43:15854` returns, `43:15918` deliveries, `43:15982` requests | doc pickers |
| 232:22293 | Pickers catalogue | `118:20306` Product, `43:16305` UOM, `118:28124` Location, `118:28054` Reference | catalogue pickers |
| 232:22294 | Pickers reasons | `118:20236` Reason, `43:16175` Stock, `43:16240` Return, `43:16370` Expense, `118:28194` Note | reason pickers |
| 232:22295 | Pickers amounts | `123:21729` Amount now, `123:21794` Doc amount, `123:21859` Line qty, `123:21924` Price, `123:21989` Stock qty | amount pickers |

Generated during Trading inventory parity implementation.
