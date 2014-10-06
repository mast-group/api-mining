# Plot itemset scaling cup vs Spark 
import matplotlib.pyplot as plt
import numpy as np

linear_trans = [1E2,1E3, 1E4, 1E5, 1E6]
s1_trans = [400, 4141, 40584]
s1_time = [295.53, 1489.781, 11713.071]
s4_trans = [426, 4024, 40774]
s4_time = [241.368, 1551.997, 9363.031]
s16_trans = [409, 4141, 40922, 408804]
s16_time = [283.045, 450.297, 1441.06, 12482.686]
s64_trans = [389, 4130, 41012, 408389, 4088830]
s64_time = [405.91, 433.18, 979.98, 5387.25,13256.16]
s128_trans = [404, 4036, 40928]
s128_time = [729.77,728.14,1060.51]

plt.figure(1)
plt.hold(True)
plt.plot(s1_trans,s1_time,'.-',linewidth=2,markersize=12)
plt.plot(s4_trans,s4_time,'.-',linewidth=2,markersize=12)
plt.plot(s16_trans,s16_time,'.-',linewidth=2,markersize=12)
plt.plot(s64_trans,s64_time,'.-',linewidth=2,markersize=12)
plt.plot(s128_trans,s128_time,'.-',linewidth=2,markersize=12)
plt.plot(linear_trans,linear_trans,'k--',linewidth=2)
plt.gca().set_xscale('log')
plt.gca().set_yscale('log')
plt.title('Transaction Scaling (10 itemsets, 93 items)')
plt.xlabel('No. Transactions')
plt.ylabel('Time (s)')
plt.legend(['1 Core','4 Cores','16 Cores','64 Cores','128 Cores','Linear'],'upper left')
#plt.xlim([0,1])
#plt.ylim([-250,max(spark_time)])
plt.grid(True)

'''
s64_itemsets = [30, 50, 100]
s64_sparsity = [5.11, 7.50, 14.53]
s64_time = [17.582133333333335*60, 2782.39,  6637.89]

plt.figure(2)
plt.hold(True)
plt.plot(s64_itemsets,s64_time,'.-',linewidth=2,markersize=12)
#plt.plot(s64_sparsity,s64_time,'.-',linewidth=2,markersize=12)
plt.title('Itemset Scaling (1,000 transactions)')
plt.xlabel('No. Itemsets')
#plt.xlabel('Average Items per Transaction')
plt.ylabel('Time (s)')
plt.legend(['64 cores'],'lower right')
plt.grid(True)
'''

plt.show()
