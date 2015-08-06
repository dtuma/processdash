select distinct pe.key
from ProcessEnactment as pe
where pe.rootItem.key = pe.includesItem.key
and pe.rootItem.project.key in (?);

select pe.key,
    pe.rootItem.wbsElement.name,
    sum(task.planTimeMin),
    sum(task.actualTimeMin),
    max(task.actualCompletionDateDim.key)
from ProcessEnactment pe, ProcessEnactment pi, TaskStatusFact as task
where pe.key in (?)
    and pe.rootItem.key = pi.rootItem.key
    and pe.process.key = pi.process.key
    and pi.includesItem.key = task.planItem.key
group by pe.key
having max(task.actualCompletionDateDim.key) < 99990000
order by pe.rootItem.wbsElement.name;

select pe.key,
    size.sizeMetric.shortName,
    size.measurementType.name,
    sum(size.addedAndModifiedSize)
from ProcessEnactment pe, ProcessEnactment pi, SizeFact as size
where pe.key in (?)
    and pe.rootItem.key = pi.rootItem.key
    and pe.process.key = pi.process.key
    and pi.includesItem.key = size.planItem.key
group by pe.key,
    size.sizeMetric.shortName,
    size.measurementType.name
order by size.measurementType.name,
    sum(size.addedAndModifiedSize) desc;
