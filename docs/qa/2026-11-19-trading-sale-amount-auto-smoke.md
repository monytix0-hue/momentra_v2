# Smoke: Trading Sale/Purchase amount auto-calc (Figma TRD-QA-01)

## Android

1. Open Trading CREATE → Record Sale → **Itemized**.
2. Pick a product with a sell price → **Sale amount** shows price × qty (default qty 1) with **Auto · tap to edit**.
3. Change qty → amount updates while Auto.
4. Edit Sale amount → shows **Edited**; changing qty does not overwrite.
5. Clear Sale amount → Auto returns.
6. **+ Add item to sale** twice → cart shows line totals + read-only sum + caption *Itemized total is read-only · sum of lines*.
7. Save → amount posted equals cart (+ pending) sum.
8. Repeat for **Record Purchase** (Purchase amount / + Add item to purchase).

## iOS

Same checklist as Android on Trading Sale and Purchase itemized sheets.
