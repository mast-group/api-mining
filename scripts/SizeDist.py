from __future__ import division
from itertools import groupby
from random import random
import matplotlib.pyplot as plt
import scipy.stats as stats
import numpy as np
import re
''' Get size distribution for itemsets '''

logdir = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/'
logfile = logdir + 'plants-06.10.2014-17:27:35-CombSupp.log'

f = open(logfile,'r')

setsizes = []
setprobs = []
itemsets = []
items = []

#for i, line in enumerate(f.readlines()): 
#    if i == 412:
#        itemsets = re.findall('{(.+?)}', line)
#        for itemset in itemsets:
#            elems = itemset.split(',')
#            setsizes.append(len(elems))
#        itemsetprobs = re.findall('=([-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)',line)
#        for setprob in itemsetprobs:    
#            setprobs.append(float(setprob[0]))


found = False
for line in f.readlines():
    
    if found and line != '\n':
        splitline = line.split('\t')
        itemset = splitline[0]
        elems = itemset.split(',')
        elems = [int(elem.strip('{} ')) for elem in elems]
        itemsets = itemsets + [elems]
        items = items + elems
        setsizes.append(len(elems))
        prob = splitline[1].split(':')[1].strip()
        setprobs.append(float(prob))
        
    if 'INTERESTING ITEMSETS' in line:
        found = True     
        
f.close()

# Histogram of itemset sizes
print 'No. itemsets: ' + str(len(setsizes))
plt.figure()
setsizes = np.array(setsizes)
plt.hist(setsizes, bins=50)
plt.title(logfile.split('/')[-1].split('-')[0] + ' - Itemset Sizes')
plt.xlabel('Itemset Size')    

# Fit Shifted Geometric
plt.figure()
shape = 1./np.mean(setsizes)
print 'p: ' + str(shape)
samp = stats.geom.rvs(shape,size=len(setsizes))
plt.hist(samp, bins=50)
plt.title('Fitted Geometric')
plt.figure()
stats.probplot(setsizes, sparams=(shape,0), dist='geom',plot=plt)
plt.title('Itemset Sizes Probplot')

# Histogram of items
plt.figure()
items = np.array(items)
plt.hist(items, bins=50, color='r')
plt.title(logfile.split('/')[-1].split('-')[0] + ' - Items')
plt.xlim([min(items),max(items)])
plt.xlabel('Items')   

# Histogram of itemset probs
plt.figure()
setprobs = np.array(setprobs)
plt.hist(setprobs, bins=50, color='g')
plt.title(logfile.split('/')[-1].split('-')[0] + ' - Itemset Probabilities')
plt.xlabel('Itemset Prob')   
#print '\np min: ' + str(np.min(setprobs))
#print 'p max: ' + str(np.max(setprobs))

# Fit Log Normal
plt.figure()
shape, loc, scale = stats.lognorm.fit(setprobs,floc=0)
print 'mu: ' + str(np.log(scale))
print 'sigma: ' + str(shape)
samp = stats.lognorm.rvs(shape,loc=loc,scale=scale,size=len(setprobs))
plt.hist(samp, bins=50)
plt.title('Fitted Log Normal')
plt.figure()
stats.probplot(setprobs, sparams=(shape,loc,scale), dist='lognorm',plot=plt)
plt.title('Itemset Probs Probplot')

# Generate exact synthetic dataset 
f = open('/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/plants_exact_synthetic.dat','w')
count = 0
while(count < 34781):
    added = False
    for i in range(len(itemset)):
        if(random() < setprobs[i]):
            f.write(' '.join(map(str,itemsets[i])))
            added = True
    if(added):        
        f.write('\n')        
        count = count +1
f.close()

plt.show()
