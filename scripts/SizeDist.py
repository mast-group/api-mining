from __future__ import division
import matplotlib.pyplot as plt
import scipy.stats as stats
import numpy as np
#import re
''' Get size distribution for itemsets '''

logdir = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/'
logfile = logdir + 'plants-CombSupp-24.09.2014-17:20:05.log'

f = open(logfile,'r')

setsizes = []
setprobs = []

#for i, line in enumerate(f.readlines()): 
#    if i == 511:
#        itemsets = re.findall('{(.+?)}', line)
#        for itemset in itemsets:
#            setsizes.append(len(itemset))

found = False
for line in f.readlines():
    
    if found and line != '\n':
        splitline = line.split('\t')
        itemset = splitline[0]
        elems = itemset.split(',')
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

# Fit Log Normal
plt.figure()
shape, loc, scale  = stats.lognorm.fit(setsizes,floc=0)
print 'mu: ' + str(np.log(scale))
print 'sigma: ' + str(shape)
samp = stats.lognorm.rvs(shape,loc=loc,scale=scale,size=len(setsizes))
plt.hist(samp, bins=50)
#plt.xlim([0,80])
plt.title('Fitted Log Normal')

# Probability plot
plt.figure()
stats.probplot(setsizes, sparams=(shape,loc,scale), dist='lognorm',plot=plt)
plt.title('Itemset Sizes Probplot')

# Histogram of itemset probs
plt.figure()
setprobs = np.array(setprobs)
plt.hist(setprobs, bins=50, color='g')
plt.title(logfile.split('/')[-1].split('-')[0] + ' - Itemset Probabilities')
plt.xlabel('Itemset Prob')   
print '\np min: ' + str(np.min(setprobs))
print 'p max: ' + str(np.max(setprobs))

plt.show()
