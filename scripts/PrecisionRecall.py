import matplotlib.pyplot as plt
from numpy import array, arange, nan

def main():
    
    baseFolder = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/'
    algs = ['ISM-Int','ISM-Prob','MAPO','UP-Miner']
    ubound = 'Dataset'
    markers = ['o','D','s','v','^','*','x','+','d','.']
    
    for alg in algs:
    
        seqPrecision = []
        seqRecall = []
        mprecision = []
        mrecall = []
        redundancy = []
        spuriousness = []
        
        fl = open(baseFolder+alg+'.pr','r')
        for line in fl:
            vals = line.split(',')
            seqPrecision.append(float(vals[0]))
            seqRecall.append(float(vals[1]))
            mprecision.append(float(vals[2]))
            mrecall.append(float(vals[3]))
            redundancy.append(float(vals[4]))
            spuriousness.append(float(vals[5]))
        fl.close()    
        
        symbol = markers[algs.index(alg)]
        plotfigpr(seqPrecision,seqRecall,'Sequence','Average Sequence Recall','Interpolated Average Sequence Precision',symbol,1) 
        plotfigpr(mprecision,mrecall,'Method','Average Method Recall','Interpolated Average Method Precision',symbol,2)
        plotfig(arange(0,501,10),redundancy,'Redundancy','top N','Average Inter-sequence Distance',symbol,3)
        plotfig(arange(0,501,10),spuriousness,'Spuriousness','top N','Average no. containing subsequences',symbol,4)
        
    fu = open(baseFolder+ubound+'.pr','r')
    vals = fu.readline().split(',')
    mRecallMax = float(vals[3])
    fu.close()    
    plotdashed(mRecallMax, 0.2,2)

    plt.figure(1)
    plt.legend(algs,loc='lower right')
    plt.figure(3)
    plt.legend(algs,loc='upper right')
    plt.figure(4)
    plt.legend(algs,loc='upper right')
    plt.figure(2)
    plt.legend(algs,loc='lower right')
    
    plt.show()

# Interpolate precision
def pinterp(prarray,recall):

    m = [p for (p,r) in prarray if r >= recall]
    if(len(m)==0):
        return nan
    else:
        return max(m) 

# Plot figure
def plotfigpr(precision,recall,title,xlabel,ylabel,symbol,figno):

    x = arange(0,1.1,0.01)
    y = [pinterp(zip(precision,recall),r) for r in x]

    plt.figure(figno)
    plt.hold(True)
    plt.plot(x,y,'.-',linewidth=2,markersize=3,marker=symbol)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.xlim([0,1])
    plt.grid(True)

# Plot dashed horizontal
def plotdashed(maxRecall,ulim,figno):

    plt.figure(figno) 
    plt.plot(array([maxRecall,maxRecall]),array([0,1]),'k--',linewidth=2) 
    plt.xlim([0,ulim])

# Plot figure
def plotfig(x,y,title,xlabel,ylabel,symbol,figno):

    plt.figure(figno)
    plt.hold(True)
    plt.plot(x,y,'.-',linewidth=2,markersize=3,marker=symbol)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.grid(True)

main()