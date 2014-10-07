from __future__ import division
'''
Compare sparsity of transaction databases
'''
import os

f1 = open('/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Datasets/Plants/plants.dat')
f2 = open('/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/plants_exact_synthetic.dat')
f3 = open('/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/plants_synthetic.dat')

for f in f1,f2,f3:

    print '\n' + os.path.basename(f.name)
    print '=============='
    
    sparsity = 0
    allitems = set()
    lines = f.readlines()
    for line in lines:
        items = line.strip().split(' ')
        sparsity += len(items)
        for item in items:
            allitems.add(int(item))
    sparsity /= len(lines)
    
    print 'Items: ' + str(len(allitems))
    print 'Transactions: ' + str(len(lines))
    print 'Avg. items per transaction: ' + str(sparsity)
    