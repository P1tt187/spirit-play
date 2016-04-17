$('#course-select').change( () ->
  courseSelection = $(this).val()
  $('#news-group').empty()
  window.maxNewsNum=-1
  $('#streamselector').attr('src','/newsstream/-1/-1/' + courseSelection)
)

refreshStreamSelector = () ->
  if "-1" == $('#course-select').val()
    setTimeout( () ->
      $('#streamselector').attr('src', '/newsstream/-1/'  + window.maxNewsNum +  '/-1')
      refreshStreamSelector()
    ,600000)

$('#streamselector').on('load', () ->
  refreshStreamSelector()
)

$(window).load( () ->  
  $('#streamselector').delay(5000).queue( () ->
    $(this).attr('src','/newsstream/-1/'  + window.maxNewsNum +  '/-1')
  )
)