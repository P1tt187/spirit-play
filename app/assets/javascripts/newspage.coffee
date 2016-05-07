$('#course-select').change( () ->
  courseSelection = $(this).val()
  $('#news-group').empty()
  window.maxNewsNum=-1

  $('#streamselector').attr('src',getnewsPrefix() + '/-1/-1/' + courseSelection)
)

getnewsPrefix = () ->
  streamsrc = $('#streamselector').attr('src')
  sourceStr = 'newsstream'
  prefix = streamsrc.substr(0,streamsrc.indexOf(sourceStr) + sourceStr.length)
  return prefix

refreshStreamSelector = () ->
  if "-1" == $('#course-select').val()
    setTimeout( () ->
      $('#streamselector').attr('src', getnewsPrefix() + '/-1/'  + window.maxNewsNum +  '/-1')
      refreshStreamSelector()
    ,600000)

$('#streamselector').on('load', () ->
  refreshStreamSelector()
)

$(window).load( () ->
  if window.autoloadEnabled == true
    $('#streamselector').delay(5000).queue( () ->
      $(this).attr('src',getnewsPrefix() + '/-1/'  + window.maxNewsNum +  '/-1')
    )
)