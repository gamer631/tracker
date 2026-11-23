"""Historical statistical summaries for recorded crash multipliers."""
import math
from typing import Iterable

def wilson_interval(successes:int,total:int,z:float=1.96)->tuple[float,float]:
    if total<=0:return (0.0,0.0)
    p=successes/total; den=1+z*z/total
    center=(p+z*z/(2*total))/den
    margin=z*math.sqrt(p*(1-p)/total+z*z/(4*total*total))/den
    return max(0,center-margin),min(1,center+margin)

def summarize(values:Iterable[float])->dict:
    vals=[float(v) for v in values if float(v)>0]
    if not vals:return {"count":0,"mean":None,"median":None,"min":None,"max":None,"low_rate":None,"high_rate":None,"streak_low":0,"streak_high":0}
    s=sorted(vals); n=len(vals); median=s[n//2] if n%2 else (s[n//2-1]+s[n//2])/2
    low=sum(v<2 for v in vals)/n; high=sum(v>=10 for v in vals)/n
    ls=0
    for v in reversed(vals):
        if v<2:ls+=1
        else:break
    hs=0
    for v in reversed(vals):
        if v>=10:hs+=1
        else:break
    return {"count":n,"mean":sum(vals)/n,"median":median,"min":s[0],"max":s[-1],"low_rate":low,"high_rate":high,"streak_low":ls,"streak_high":hs}

def target_rate(values:Iterable[float],target:float)->dict:
    vals=[float(v) for v in values if float(v)>0]; n=len(vals); hits=sum(v>=target for v in vals)
    lo,hi=wilson_interval(hits,n)
    return {"target":target,"sample":n,"hits":hits,"rate":hits/n if n else None,"ci_low":lo,"ci_high":hi}

def rolling_backtest(values:list[float],target:float,window:int=100)->dict:
    vals=[float(v) for v in values if float(v)>0]
    if len(vals)<2:return {"test_n":0,"hits":0,"rate":None,"baseline_mean":None}
    hits=0; test_n=0; baseline=[]
    for i in range(max(1,len(vals)-window),len(vals)):
        hist=vals[max(0,i-window):i]
        if hist:
            baseline.append(sum(x>=target for x in hist)/len(hist))
            hits+=int(vals[i]>=target); test_n+=1
    return {"test_n":test_n,"hits":hits,"rate":hits/test_n if test_n else None,"baseline_mean":sum(baseline)/len(baseline) if baseline else None}

def window_summaries(values:list[float],windows=(10,25,50,100))->dict:
    return {w:summarize(values[-w:]) for w in windows}
