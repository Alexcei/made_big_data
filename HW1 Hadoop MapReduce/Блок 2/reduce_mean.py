import sys


ck1 = 0
mk1 = 0
for row in sys.stdin:
	try:
		ck2, mk2 = map(float, row.split())
		mk1 = (ck1 * mk1 + ck2 * mk2) / (ck1 + ck2)
		ck1 += ck2
	except:
		continue

print(f'{ck1} {mk1}')

with open('result_diff.txt', 'a') as file:
	file.write(f'Result MapReduce mean: {mk1}\n')
