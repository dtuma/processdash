select d.foundDateDim.fullDate, sum(d.fixCount)
from DefectLogFact as d
group by d.foundDateDim.fullDate
order by d.foundDateDim.fullDate
