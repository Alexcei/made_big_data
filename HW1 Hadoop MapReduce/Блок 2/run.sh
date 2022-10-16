#bin/bash
rm result_diff.txt
python clean_code.py
cat AB_NYC_2019.csv | python mapper_mean.py | python reduce_mean.py
cat AB_NYC_2019.csv | python mapper_var.py | python reduce_var.py
cat result_diff.txt
