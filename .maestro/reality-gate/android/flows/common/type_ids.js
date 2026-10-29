// Maps the fixture's record type to the app's Quick Add tile test id
// (MaestroIds QA_TILE_*), so tiles are opened by id instead of by scrolling
// to a matching label.
var map = {
  Spend: 'personal_expense_fab',
  Income: 'qa.tile.income',
  Transfer: 'qa.tile.transfer',
  Savings: 'qa.tile.savings',
  Mood: 'qa.tile.mood',
  Recovery: 'qa.tile.recovery',
  Attention: 'qa.tile.attention',
  Milestone: 'qa.tile.milestone',
  Opportunity: 'qa.tile.opportunity',
  Pivot: 'qa.tile.pivot',
  Progress: 'qa.tile.progress',
  Learning: 'qa.tile.learning',
  Experience: 'qa.tile.experience',
  Wellbeing: 'qa.tile.wellbeing',
  Discovery: 'qa.tile.discovery',
  Create: 'qa.tile.expression',
  Connection: 'qa.tile.connection',
  Support: 'qa.tile.support',
  'Shared Exp': 'qa.tile.shared_exp',
  Investment: 'qa.tile.investment',
}
output.tileId = map[output.r.type]