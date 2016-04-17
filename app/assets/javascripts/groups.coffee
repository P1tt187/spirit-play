courseSelector = () ->
  courseSelection = $('#course-select').val()
  $('.group').addClass('hidden')
  $('.' + courseSelection).removeClass('hidden')
  if typeof localStorage != "undefined"
    localStorage.setItem('lastCourse',$('#course-select').val())

$('#course-select').change( courseSelector)
$(window).load( () ->
  if typeof localStorage != "undefined"
    lastCourse = localStorage.getItem("lastCourse")
    if `lastCourse == null`
      lastCourse = $('#course-select').first().val()

  courseSelector()
)