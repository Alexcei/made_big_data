import sys
import csv


prices = []
with open('AB_NYC_2019.csv', encoding='utf-8', newline='') as file:
	reader = csv.DictReader(file)
	for row in reader:
		try:
			prices.append(int(row['price']))
		except:
			continue

ck = len(prices)
mk = sum(prices) / ck

vk = sum([(mk - i)**2 for i in prices]) / ck
print(f'{ck} {mk} {vk}')

with open('result_diff.txt', 'a') as file:
	file.write(f'Clean code mean: {mk} var: {vk}\n')
