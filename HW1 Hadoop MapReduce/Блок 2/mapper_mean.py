import sys
import csv


ck = 0
mk = 0
for row in csv.reader(sys.stdin):
	try:
		mk += int(row[9])
		ck += 1
	except:
		continue

print(f'{ck} {mk / ck}')



# # for check
# # python mapper_mean.py | python reduce_mean.py

# import sys
# import csv


# ck = 0
# mk = 0
# with open('AB_NYC_2019.csv', encoding='utf-8', newline='') as file:
# 	reader = csv.DictReader(file)
# 	for row in reader:
# 		try:
# 			mk += int(row['price'])
# 			ck += 1
# 		except:
# 			continue

# print(f'{ck} {mk / ck}')
