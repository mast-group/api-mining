''' Plot top Uganda itemsets as a time series '''
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import numpy as np
import linecache as lc
import pickle
from datetime import datetime
from scipy.stats import itemfreq

top_itemsets = 6

basedir = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/'
logfile = basedir + 'Logs/IIM-uganda_en_3m_filtered-14.01.2015-16:36:50.log'
db_file = basedir + 'Datasets/Uganda/3mths/uganda_en_filtered.dat'
dates_file = basedir + 'Datasets/Uganda/3mths/dates_en_filtered.txt'
item_dict = basedir + 'Datasets/Uganda/items_en.dict'

itemsets = []
setprobs = []

# Get top itemsets
found = False
count = 0
f = open(logfile,'r')
for line in f:
    
    if found and line != '\n':
        splitline = line.split('\t')
        itemset = splitline[0]
        elems = itemset.split(',')
        if(len(elems)==1): # ignore singletons
            continue
        elems = [elem.strip('{} ') for elem in elems]
        itemsets = itemsets + [elems]
        prob = splitline[1].split(':')[1].strip()
        setprobs.append(float(prob))
        count += 1
        if count == top_itemsets:
            break    
        
    if 'INTERESTING ITEMSETS' in line:
        found = True     
        
f.close()

# Get itemset occurence days
itemset_days = [[] for i in range(top_itemsets)]

db = open(db_file,'r')
index = 1
for line in db:
    transaction_items = line.strip().split(' ')
    for i in range(len(itemsets)):
        if set(itemsets[i]) < set(transaction_items):
            date = datetime.strptime(lc.getline(dates_file,index).strip(),'%Y-%m-%d %H:%M:%S')
            day = datetime.strptime(datetime.strftime(date,'%Y-%m-%d'),'%Y-%m-%d')  # convert to just day
            itemset_days[i].append(day)
    index += 1
    
db.close()

# Decode itemsets back to words
words = pickle.load(open(item_dict,'r'))
legend = [] 
for itemset in itemsets:
    decodedItems = [words.get(int(item)) for item in itemset]
    decodedItemset = ', '.join(decodedItems)
    legend.append(decodedItemset)

# Aggregate itemsets by day and plot across time
for i in range(len(itemsets)):
    
    freqs = itemfreq(np.array(itemset_days[i]))
    print freqs
    plt.plot(freqs[:,0],freqs[:,1])

plt.ylabel('Mentions per day')
plt.gca().xaxis.set_major_formatter(mdates.DateFormatter('%m/%Y'))
plt.gca().xaxis.set_major_locator(mdates.MonthLocator())
plt.gca().xaxis.set_minor_locator(mdates.WeekdayLocator())
plt.grid()
plt.gcf().autofmt_xdate()

# Shrink current axis's height by 10% on the top
# http://stackoverflow.com/questions/4700614/how-to-put-the-legend-out-of-the-plot
ax = plt.gca()
box = ax.get_position()
ax.set_position([box.x0, box.y0 - box.height * 0.1,
                 box.width, box.height * 0.9])
# Put a legend below current axis
ax.legend(legend,loc='lower center', bbox_to_anchor=(0.5005, 1.05),
          fancybox=True, ncol=3)

plt.show()

    