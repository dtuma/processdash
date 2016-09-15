select distinct pe.rootItem.key
from ProcessEnactment as pe
where pe.rootItem.key = pe.includesItem.key
and pe.rootItem.project.key in (?);

select pe.rootItem.key,
    pe.rootItem.wbsElement.name,
    sum(task.planTimeMin),
    sum(task.actualTimeMin),
    max(task.actualCompletionDateDim.key)
from ProcessEnactment pe, TaskStatusFact as task
where pe.rootItem.key in (?)
    and pe.includesItem.key = task.planItem.key
group by pe.rootItem.key
having max(task.actualCompletionDateDim.key) < 99990000
order by pe.rootItem.wbsElement.name;

select pe.rootItem.key,
    size.sizeMetric.shortName,
    size.measurementType.name,
    sum(size.addedAndModifiedSize)
from ProcessEnactment pe, SizeFact as size
where pe.rootItem.key in (?)
    and pe.includesItem.key = size.planItem.key
    and size.measurementType.name in ('Plan', 'Actual')
group by pe.key,
    size.sizeMetric.shortName,
    size.measurementType.name
order by size.measurementType.name,
    sum(size.addedAndModifiedSize) desc;
