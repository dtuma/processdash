select d.foundDateDim.fullDate, count(*)
from DefectLogFact as d
group by d.foundDateDim.fullDate
order by d.foundDateDim.fullDate
