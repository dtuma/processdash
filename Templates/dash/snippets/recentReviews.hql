select task.planItem.key,
    task.planItem.project.name,
    task.planItem.wbsElement.name,
    task.planItem.task.name,
    task.planTimeMin,
    task.actualTimeMin,
    task.actualCompletionDate,
    task.dataBlock.person.encryptedName,
    task.dataBlock.person.key,
    task.planItem.identifier,
    task.planItem.parent.key,
    mapsTo.shortName
from TaskStatusFact as task
join task.planItem.phase.mapsToPhase mapsTo
where mapsTo.process.identifier = ?
  and mapsTo.shortName in (?)
  and task.actualCompletionDate is not null
order by task.actualCompletionDate;

select defect.planItem.key, count(*)
from DefectLogFact as defect
where defect.planItem.key in (?)
group by defect.planItem.key;

select count(*)
from DefectLogFact as defect
join defect.removedPhase.mapsToPhase mapsTo
where defect.planItem.key = ?
  and mapsTo.shortName = ?;
