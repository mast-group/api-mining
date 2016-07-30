import matplotlib.pyplot as plt
from matplotlib import rc
from numpy import array, arange, nan
import os

def main():

    
    
    baseFolder = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/API/examples/all/'
    algs = ['ISM-Prob','MAPO','UPMiner']
    ubound = 'Dataset'
    markers = ['o','D','s','v','^','*','x','+','d','.']
    rc('axes',color_cycle=['b','g','r','m'])

    rc('ps', fonttype=42)
    rc('pdf', fonttype=42)

    rc('xtick', labelsize=16) 
    rc('ytick', labelsize=16) 		    

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
        plotfigpr(seqPrecision,seqRecall,'','Average Sequence Recall','Interpolated Average Sequence Precision',symbol,1) 
        plotfigpr(mprecision,mrecall,'Method','Average Method Recall','Interpolated Average Method Precision',symbol,2)
        plotfig(arange(10,501,10),redundancy,'Redundancy','top N','Average Inter-sequence Distance',symbol,3)
        plotfig(arange(10,501,10),spuriousness,'','top N','Average no. containing subsequences',symbol,4)
        
    fu = open(baseFolder+ubound+'.pr','r')
    vals = fu.readline().split(',')
    print 'Average Dataset Sequence Precision: ' + vals[0]
    print 'Averaga Dataset Sequence Recall: ' + vals[1]
    print 'Average Min. Edit Distance Examples/Test calls to Dataset: ' + vals[4]
    mRecallMax = float(vals[3])
    fu.close()   
    plotdashed(mRecallMax,2)

    algs[algs.index('ISM-Prob')] = 'PAM'
    plt.figure(1)
    plt.legend(algs,loc='lower right')
    plt.savefig(baseFolder+'SequencePrecisionRecall.png',dpi=150)
    savepdf('SequencePrecisionRecall',baseFolder)
    plt.figure(2)
    plt.legend(algs,loc='lower right')
    plt.savefig(baseFolder+'MethodPrecisionRecall.png',dpi=150)
    plt.figure(3)
    plt.legend(algs,loc='lower right')
    plt.savefig(baseFolder+'InterSeqDistance.png',dpi=150)
    plt.figure(4)
    plt.legend(algs,loc='upper left')
    plt.savefig(baseFolder+'ContainingSubseqs.png',dpi=150)
    savepdf('ContainingSubseqs',baseFolder)
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
    plt.plot(x,y,'.-',linewidth=2,markersize=5,marker=symbol,clip_on=False)
    plt.xlabel(xlabel,fontsize=16)
    plt.ylabel(ylabel,fontsize=16)
    plt.title(title)
    plt.xlim([0,0.5])
    plt.ylim([0,0.5])
    plt.grid(True)

# Plot dashed horizontal
def plotdashed(maxRecall,figno):

    plt.figure(figno) 
    plt.plot(array([maxRecall,maxRecall]),array([0,1]),'k--',linewidth=2) 

# Plot figure
def plotfig(x,y,title,xlabel,ylabel,symbol,figno):

    plt.figure(figno)
    plt.hold(True)
    plt.plot(x,y,'.-',linewidth=2,markersize=5,marker=symbol,clip_on=False)
    plt.xlabel(xlabel,fontsize=16)
    plt.ylabel(ylabel,fontsize=16)
    plt.title(title)
    plt.grid(True)

def savepdf(name,folder):
    plt.savefig(folder+name+'.eps',format='eps',dpi=1200)
    os.system('epstopdf '+folder+name+'.eps')
    os.system('pdfcrop '+folder+name+'.pdf '+folder+name+'.pdf')
    os.system('rm '+folder+name+'.eps')

main()
