# Plot itemset precision-recall
import matplotlib.pyplot as plt
import numpy as np

# Data
dct = {}
# IIM
dct['IIM_iterations'] = [50, 100, 150, 200]
dct['IIM_time'] = [29.866, 47.677, 82.376, 163.81]
dct['IIM_precision'] = [0.7244094488188977, 0.6802721088435374, 0.6728395061728395, 0.8451612903225807]
dct['IIM_recall'] = [0.0, 0.0, 0.0, 1.0]
# MTV
dct['MTV_iterations'] = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 150, 150, 160, 170, 180, 190, 200]
dct['MTV_time'] =  [7.259, 53.318, 62.121, 66.194, 68.055, 72.098, 73.874, 76.809, 79.43, 80.392, 80.623, 82.06, 80.488, 80.409, 80.5, 80.398, 81.247, 80.36, 80.425, 80.572]
dct['MTV_precision'] =  [0.4, 0.45, 0.4583333333333333, 0.5185185185185185, 0.5357142857142857, 0.5517241379310345, 0.5666666666666667, 0.5806451612903226, 0.59375, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061, 0.6060606060606061]
dct['MTV_recall'] = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
# FIM
dct['FIM_iterations'] = [10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 150, 150, 160, 170, 180, 190, 200]
dct['FIM_time'] = [0.451, 0.283, 0.267, 0.269, 0.252, 0.218, 0.231, 0.233, 0.225, 0.211, 0.197, 0.186, 0.191, 0.185, 0.196, 0.187, 0.187, 0.198, 0.185, 0.189]
dct['FIM_precision'] = [0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405, 0.0062707798493329405]
dct['FIM_recall'] = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]

def main():
    
    probname = 'caviar'
    name = ''
    
    cols = ['r','g','b']
    prefixes = ['IIM','MTV','FIM']
    
    for prefix in prefixes:
    
        precision = dct[prefix+'_precision']
        recall = dct[prefix+'_recall']
        x = dct[prefix+'_iterations']
        #plotfigpr(precision,recall,probname,name,1)
        plotfigpandr(x,precision,recall,probname,name,cols[prefixes.index(prefix)],2)
        
    #plt.legend(['IIM','MTV','FIM'],'lower right')
    plt.legend(['IIM Precision','IIM Recall','MTV Precision','MTV Recall','FIM Precision','FIM Recall'],'lower right')
    plt.show()
    

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

def plotfigpandr(x,precision,recall,probname,name,col,figno):
    
    plt.figure(figno)
    plt.hold(True)
    plt.plot(x,precision,'.-',linewidth=2,markersize=12,color=col)
    plt.plot(x,recall,'.--',linewidth=2,markersize=12,color=col)
    plt.title(probname+' Precison-Recall - '+name)
    plt.xlabel(name)
    plt.ylabel('Precision/Recall')
    #plt.legend(['Precision','Recall'],'lower left')
    plt.xlim([1,max(x)])
    plt.ylim([-0.1,1.1])
    plt.grid(True)

main()
