# Plot itemset precision-recall
import matplotlib.pyplot as plt
import numpy as np

# Data
dct = {}
# Caviar IIM
dct['caviar','IIM_freq'] = [5, 10, 15, 20, 25, 30]
dct['caviar','IIM_time'] = [165.353, 192.229, 216.013, 274.778, 278.89, 327.344]
dct['caviar','IIM_precision'] = [0.8231292517006803, 0.8269230769230769, 0.8875, 0.8392857142857143, 0.8698224852071006, 0.8406593406593407]
dct['caviar','IIM_recall'] = [1.0, 1.0, 1.0, 1.0, 1.0, 1.0]
# Caviar MTV
dct['caviar','MTV_freq'] = [5, 10, 15, 20, 25, 30]
dct['caviar','MTV_time'] =  [122.49, 157.908, 143.913, 172.112, 245.534, 210.675]
dct['caviar','MTV_precision'] = [0.8076923076923077, 0.7368421052631579, 0.75, 0.75, 0.6779661016949152, 0.6779661016949152]
dct['caviar','MTV_recall'] = [0.0, 0.6, 0.4, 0.3, 0.64, 0.5333333333333333]
# Caviar FIM
dct['caviar','FIM_freq'] = [5, 10, 15, 20, 25, 30]
dct['caviar','FIM_time'] = [459.554, 457.651, 337.394, 364.454, 213.731, 351.969]
dct['caviar','FIM_precision'] = [4.992958529584043E-6, 4.433611673866493E-6, 5.0114072938309115E-6, 5.202803805836119E-6, 7.89697175181709E-6, 5.223195791283624E-6]
dct['caviar','FIM_recall'] = [0.6, 0.8, 0.6666666666666666, 0.65, 0.64, 0.6]
# Freerider IIM
dct['freerider','IIM_freq'] = [5, 10, 15, 20, 25, 30]
dct['freerider','IIM_time'] = [219.911, 296.536, 356.288, 510.355, 574.817, 673.445]
dct['freerider','IIM_precision'] = [0.8356164383561644, 0.7975460122699386, 0.8571428571428571, 0.8430232558139535, 0.8857142857142857, 0.8135593220338984]
dct['freerider','IIM_recall'] = [1.0, 1.0, 1.0, 1.0, 1.0, 1.0]
# Freerider MTV
dct['freerider','MTV_freq'] = [5, 10, 15, 20, 25, 30]
dct['freerider','MTV_time'] = [210.194, 505.613, 1338.909, 3038.599, 10112.52, 33095.181]
dct['freerider','MTV_precision'] = [0.8928571428571429, 0.9117647058823529, 0.8974358974358975, 0.9090909090909091, 0.9183673469387755, 0.94]
dct['freerider','MTV_recall'] =  [1.0, 1.0, 1.0, 1.0, 1.0, 0.9333333333333333]
# Freerider FIM
dct['freerider','FIM_freq'] = [5, 10, 15, 20, 25, 30]
#dct['freerider','FIM_time'] = 
#dct['freerider','FIM_precision'] = 
#dct['freerider','FIM_recall'] =

def main():
    
    #probname = 'caviar'
    probname = 'freerider'
    
    cols = ['r','g','b']
    prefixes = ['IIM','MTV']
    #,'FIM']
    
    for prefix in prefixes:
    
        precision = dct[probname,prefix+'_precision']
        recall = dct[probname,prefix+'_recall']
        time = dct[probname,prefix+'_time']
        x = dct[probname,prefix+'_freq']
       
        plt.figure(1)
        plt.hold(True)
        plt.plot(x,precision,'.-',linewidth=2,markersize=12,clip_on=False)
        plt.title(probname+' precison (all)')
        plt.xlabel('Special Set Frequency')
        plt.ylabel('Precision (all)')
        plt.ylim([0,1])
        plt.grid(True)
         
        
        plt.figure(2)
        plt.hold(True)
        plt.plot(x,recall,'.-',linewidth=2,markersize=12,clip_on=False)
        plt.title(probname+' recall (special set)')
        plt.xlabel('Special Set Frequency')
        plt.ylabel('Recall (special set)')
        plt.ylim([0,1])
        plt.grid(True) 
        
        plt.figure(3)
        plt.hold(True)
        plt.plot(x,time,'.-',linewidth=2,markersize=12,clip_on=False)
        plt.title(probname+' time (s)')
        plt.xlabel('Special Set Frequency')
        plt.ylabel('Time (s)')
        #plt.gca().set_ylim(bottom=0)
        plt.grid(True) 
        
    for i in range(1,4):    
        plt.figure(i)   
        plt.legend(['IIM','MTV','FIM'],'lower right')
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
