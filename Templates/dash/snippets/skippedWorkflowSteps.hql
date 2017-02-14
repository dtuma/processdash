select
  t.planItem.identifier,
  pe.rootItem.key,
  pe.process.key,
  t.planItem.parent.key,
  t.planItem.ordinal,
  t.actualCompletionDateDim.key,
  t.dataBlock.person.encryptedName,
  t.planItem.wbsElement.name,
  t.planItem.task.name,
  t.dataBlock.person.key
from ProcessEnactment as pe, TaskStatusFact as t
join t.planItem.phase.mapsToPhase phase
where pe.includesItem.key = t.planItem.key
  and pe.process.key = phase.process.key
order by
  pe.rootItem.key,
  pe.process.key,
  t.planItem.parent.key,
  t.planItem.ordinal
