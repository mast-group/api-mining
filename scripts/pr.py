# Plot itemset precision-recall
import matplotlib.pyplot as plt
import numpy as np

def main():
    
    probname = 'cross-supp'
    name = 'difficulty'
    
    lines = readData(probname,name)
    precision = lines[2]
    recall = lines[3]
    plotfigpr(precision,recall,probname,name,1)
    
    lines = readData(probname,name,'mtv')
    precision = lines[2]
    recall = lines[3]
    plotfigpr(precision,recall,probname,name,1)
    plt.legend(['us','mtv'],'lower right')
    plt.show()
    
    #plotfigpandr(x,precision,recall,probname,name,2)
    
    '''
    name = 'robustness'
    
    lines = readData(probname,name)
    x = lines[0]
    precision = lines[2]
    recall = lines[3]
  
    plotfigpr(precision,recall,probname,name,3)
    plotfigpandr(x,precision,recall,probname,name,4)
    plt.show()
    '''

def plotfigpr(precision,recall,probname,name,figno):

    # sort
    ind = np.array(recall).argsort()
    r_d = np.array(recall)[ind]
    p_d = np.array(precision)[ind]

    plt.figure(figno)
    plt.hold(True)
    plt.plot(r_d,p_d,'.-',linewidth=2,markersize=12)
    plt.title(probname+' Precison-Recall - '+name)
    plt.xlabel('Recall')
    plt.ylabel('Precision')
    plt.xlim([0,1])
    plt.ylim([0,1])
    plt.grid(True)

def plotfigpandr(x,precision,recall,probname,name,figno):
    
    plt.figure(figno)
    plt.hold(True)
    plt.plot(x,precision,'.-',linewidth=2,markersize=12)
    plt.plot(x,recall,'.-',linewidth=2,markersize=12)
    plt.title(probname+' Precison-Recall - '+name)
    plt.xlabel(name)
    plt.ylabel('Precision/Recall')
    plt.legend(['Precision','Recall'],'lower left')
    plt.xlim([0,10])
    plt.ylim([0,1])
    plt.grid(True)

def readData(probname,name,prefix=None):
    
    lines = []
    home = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/ItemsetEval/'
    fl = ''
    if(prefix != None):
        fl +=  prefix+'_'
    fl += probname+'_'+name+'.txt'
    f = open(home + fl,'r')
    for line in nonblank_lines(f):
        lst = []
        elems = line.strip()[1:-1].split(",")
        for elem in elems:
            lst.append(float(elem.strip()))
        lines.append(lst)    
    return lines

def nonblank_lines(f):
    for l in f:
        line = l.rstrip()
        if line:
            yield line
main()
