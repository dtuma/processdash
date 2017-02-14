select
  t.planItem.identifier,
  d.value.text,
  t.planItem.project.name,
  t.planItem.wbsElement.name,
  t.planItem.task.name,
  t.dataBlock.person.encryptedName,
  t.actualCompletionDate,
  t.dataBlock.person.key
from TaskStatusFact as t,
  DataBlockAttrFact as d
where t.actualTimeMin = 0
  and t.actualCompletionDate is not null
  and t.dataBlock.key = d.dataBlock.key
  and d.attribute.identifier = 'data_block.pdash.export_filename'
order by t.actualCompletionDate
