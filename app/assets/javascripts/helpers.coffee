Date.prototype.getWeek = ( ) ->
  d = this
  # Create a copy of this date object
  target  = new Date(d.valueOf())

# ISO week date weeks start on monday
# so correct the day number
  dayNr   = (d.getDay() + 6) % 7

# Set the target to the thursday of this week so the
# target date is in the right year
  target.setDate(target.getDate() - dayNr + 3)

# ISO 8601 states that week 1 is the week
# with january 4th in it
  jan4    = new Date(target.getFullYear(), 0, 4)

# Number of days between target date and january 4th
  dayDiff = (target - jan4) / 86400000

# Calculate week number: Week 1 (january 4th) plus the
# number of weeks between target date and january 4th
  weekNr = 1 + Math.floor(dayDiff / 7)

  weekNr

weeknumber = new Date().getWeek()
$('#weekNumber').html(weeknumber)
if weeknumber % 2 == 0
  $('#even').removeClass('hidden')
  $('#week-select').val('EVEN')
else
  $('#uneven').removeClass('hidden')
  $('#week-select').val('UNEVEN')

