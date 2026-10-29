// Maps a seed record type to the Business Quick Add action that creates it.
// Business Quick Add exposes 8 record types on this build; the other 18
// fixture types (Spend, CashSale, Vendor, Issue, Approval, Team Update, ...)
// have no create form, so the chunk filter drops them before this runs.
var map = {
  'Revenue': 'Log Revenue',
  'Expense': 'Log Expense',
  'Invoice': 'Invoice Track',
  'Tax Entry': 'Tax Entry',
  'Investor Update': 'Investor Update',
  'Budget Alert': 'Budget Alert',
  'Forecast Update': 'Forecast Update',
  'General Update': 'General Update',
}
output.action = map[(output.r && output.r.type) ? output.r.type : output.TYPE] || output.TYPE