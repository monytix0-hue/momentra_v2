// Computes the Material3 calendar target for a record's date.
// Reads output.r.date (YYYY-MM-DD), or output.dateStr when run standalone.
// Writes output.whenDayDesc / output.whenMonthsBack.
// Arithmetic parsing avoids new Date(string) timezone drift.
var raw = (output.r && output.r.date) ? output.r.date : output.dateStr
var parts = raw.split('-')
var year = parseInt(parts[0], 10)
var month = parseInt(parts[1], 10) - 1
var day = parseInt(parts[2], 10)

var monthNames = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
]
var weekDays = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']

var dow = new Date(Date.UTC(year, month, day)).getUTCDay()
output.whenDayDesc = weekDays[dow] + ', ' + monthNames[month] + ' ' + day + ', ' + year

var now = new Date()
var back = (now.getFullYear() * 12 + now.getMonth()) - (year * 12 + month)
output.whenMonthsBack = Math.max(0, back)

// Header text of the Material3 month grid, e.g. "July 2026". The picker loop
// pages back until this is actually on screen instead of trusting a tap count.
output.whenMonthDesc = monthNames[month] + ' ' + year