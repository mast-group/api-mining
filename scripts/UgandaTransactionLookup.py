''' Look up transaction in  Uganda dataset by date '''
import linecache as lc
import pickle

days = ['2013-03-29','2013-01-24','2013-03-26','2013-02-07','2013-02-08','2013-02-09','2013-01-30']
phrases = [['soul','rest','peac'],['chris','brown'],['bt','nt'],['god','bless'],['god','bless'],['god','bless'],['presid','museveni']]

basedir = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/'
db_file = basedir + 'Datasets/Uganda/3mths/uganda_en_filtered.dat'
dates_file = basedir + 'Datasets/Uganda/3mths/dates_en_filtered.txt'
item_dict = basedir + 'Datasets/Uganda/items_en.dict'
words = pickle.load(open(item_dict,'r'))

for day, phrase in zip(days,phrases):

    print '\n=====Looking for day ' + day

    dates_f = open(dates_file,'r')
    index = 1
    for line in dates_f:
        if(day in line): 
            items = lc.getline(db_file,index).strip().split(' ')
            decoded_trans = []
            for item in items:
                decoded_trans.append(words.get(int(item)))
            if(all(word in decoded_trans for word in phrase)):
                print decoded_trans
        index += 1
        

        
          