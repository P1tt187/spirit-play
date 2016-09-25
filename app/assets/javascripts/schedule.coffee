
setContent = () ->
  $('#addSelectionBtn').removeClass('btn-success').addClass('btn-primary').children().removeClass('glyphicon-star').addClass('glyphicon-star-empty')

  $('.alternative, .lecture').addClass('hidden')
  $('.' + $('#course-select').val()).removeClass('hidden')
  lectureUUID = ''
  $('.' + $('#course-select').val()).each( (index,value) ->
    lectureUUID+= $(value).data('uuid')  + ';'
  )

  $('#icalInput').val(lectureUUID)
  if $('#week-select').val() != 'WEEKLY'
    if $('#week-select').val() == 'EVEN'
      $('.UNEVEN').addClass('hidden')
    if $('#week-select').val() == 'UNEVEN'
      $('.EVEN').addClass('hidden')

  if typeof localStorage != "undefined"
    localStorage.setItem('lastCourse',$('#course-select').val())
  setCalendarContent()

setCalendarContent = () ->
  currentUrl = window.location.href
  prefix = currentUrl.substr(0, currentUrl.lastIndexOf("/"))
  url = prefix + "/calendar/" + $('#course-select').val()
  $('#calendarLink').val(url)

encloseArray = (item) ->
  if $.isArray(item)
    item
  else
    Array
    [].push(item)

$('#course-select').change( setContent )
$('#week-select').change(setContent)

$('.lecture').click( () ->
  if typeof localStorage != "undefined"
    personalSchedule = encloseArray(JSON.parse(localStorage.getItem("personalSchedule") || "[]"))
    personalSchedule.push( $(this).data('uuid') )    
    localStorage.setItem("personalSchedule", JSON.stringify(personalSchedule))
    $(this).addClass('alert alert-success')
)

$('#addSelectionBtn').click( () ->
  if typeof localStorage != "undefined"
    personalSchedule = encloseArray(JSON.parse(localStorage.getItem("personalSchedule") || "[]"))
    $('.' + $('#course-select').val()).each( ()->
      personalSchedule.push($(this).data('uuid'))
    )
    localStorage.setItem("personalSchedule", JSON.stringify(personalSchedule))
    child = $(this).children()
    child.removeClass("glyphicon-star-empty").addClass("glyphicon-star")
    $(this).removeClass("btn-primary").addClass("btn-success")
)

$(window).load( () ->
  if typeof localStorage != "undefined"
    lastCourse = localStorage.getItem("lastCourse")
    if `lastCourse == null`
      lastCourse = $('#course-select').first().val()

    courseNotAllowed = true
    $('#course-select').find('option').each((index,value) ->
        if value == lastCourse
          courseNotAllowed = false
    )
    if courseNotAllowed
      lastCourse = $('#course-select').first().val()

    $('#course-select').val(lastCourse)
  setContent()
  $('[data-dayindex="' + new Date().getDay() + '"]').tab('show')
  clipboard = new Clipboard('#clipboardBtn')
  clipboard.on('success', (e) ->
    console.info('Action:', e.action);
    console.info('Text:', e.text);
    console.info('Trigger:', e.trigger);

    e.clearSelection();
   )

  clipboard.on('error', (e) ->
    console.error('Action:', e.action);
    console.error('Trigger:', e.trigger);
   )
  )
