import sys


ck1 = 0
mk1 = 0
vk1 = 0
for row in sys.stdin:
	try:
		ck2, mk2, vk2 = map(float, row.split())
		vk1 = (ck1 * vk1 + ck2 * vk2) / (ck1 + ck2) + ck1 * ck2 * ((mk1 - mk2) / (ck1 + ck2))**2
		mk1 = (ck1 * mk1 + ck2 * mk2) / (ck1 + ck2)
		ck1 += ck2
	except:
		continue

print(f'{ck1} {mk1} {vk1}')

with open('result_diff.txt', 'a') as file:
	file.write(f'Result MapReduce var: {vk1}\n')
