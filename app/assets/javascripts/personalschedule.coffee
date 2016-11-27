setContent = () ->
  $('.lecture, .alternative').addClass('hidden')
  if typeof localStorage != "undefined"
    personalSchedule = encloseArray(JSON.parse(localStorage.getItem("personalSchedule") || "[]"))

    personalSchedule.forEach( (element, pos) ->
      $('[data-uuid="' + element + '"]').removeClass('hidden')
    )
  if $('#week-select').val() != 'WEEKLY'
    if $('#week-select').val() == 'EVEN'
      $('.UNEVEN').addClass('hidden')
    if $('#week-select').val() == 'UNEVEN'
      $('.EVEN').addClass('hidden')

$('#week-select').change(setContent)

encloseArray = (item) ->
  if !$.isArray(item)
    [item]
  else
    item

$(window).on('load', () ->
  setContent()
  setIcalField()
  $('[data-dayindex="' + new Date().getDay() + '"]').tab('show');
)

$('.lecture').click( () ->
  if typeof localStorage != "undefined"
    personalSchedule = encloseArray(JSON.parse(localStorage.getItem("personalSchedule") || "[]"))
    uuid = $(this).data('uuid')
    indexes = encloseArray(personalSchedule.indexOf(uuid))
    indexes.forEach((element, pos) ->
      personalSchedule.splice(element,1)
    )
    localStorage.setItem("personalSchedule",JSON.stringify(personalSchedule))
    $(this).addClass('hidden')
    setIcalField()
)

$('#deletePrivateData').click( () ->
  if typeof localStorage != "undefined"
    localStorage.clear()
  window.location = window.location
)

setIcalField = () ->
  if typeof localStorage != "undefined"
    content = ""
    personalSchedule = encloseArray(JSON.parse(localStorage.getItem("personalSchedule") || "[]"))
    personalSchedule.forEach( (value,index) ->
      content+=value + ';'
    )
    $('#icalInput').val(content)
