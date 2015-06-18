# Plot itemset scaling cup vs Spark 
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np

rc('xtick', labelsize=16) 
rc('ytick', labelsize=16) 

trans = [1E3, 1E4]
#, 1E5, 1E6]
linear_trans = [1E1, 1E2]
#, 1E3, 1E4]
s64_time = [418.57,2386.59]
#,277.16,2715.59]

plt.figure(1)
plt.hold(True)
plt.plot(trans,s64_time,'.-',linewidth=2,markersize=12)
plt.plot(trans,linear_trans,'k--',linewidth=2)
plt.gca().set_xscale('log')
plt.gca().set_yscale('log')
#plt.title('Transaction Scaling')
plt.xlabel('No. Transactions',fontsize=16)
plt.ylabel('Time (s)',fontsize=16)
#plt.legend(['1 Core','4 Cores','16 Cores','64 Cores','128 Cores','Linear'],'upper left')
#plt.axis('equal')
plt.xlim([min(trans),max(trans)])
#plt.ylim([-250,max(spark_time)])
plt.grid(True)

plt.show()
