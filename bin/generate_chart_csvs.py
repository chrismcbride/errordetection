#!/usr/bin/env python3

# Generates CSVs per error_code used for rendering charts with d3.js. One file per
# error_code is emitted to ../static/data/
import sys
import csv
from collections import defaultdict

version_a = '5.0007.510.011'
version_b = '5.0007.610.011'

distinct_codes = set()
distinct_minutes = set()
versionA_errors_per_minute_per_code = defaultdict(lambda: defaultdict(int))
versionB_errors_per_minute_per_code = defaultdict(lambda: defaultdict(int))

# expects a csv on stdin in format of `HH:MM:SS,Build_Version,error_code`
for line in csv.reader(sys.stdin):
    timestamp_parts = line[0].split(':')
    # Rolling up data to the minute level
    minute = timestamp_parts[0] + ':' + timestamp_parts[1]
    code = int(line[2])
    distinct_codes.add(code)
    distinct_minutes.add(minute)
    if line[1] == version_a:
        versionA_errors_per_minute_per_code[minute][code] += 1
    else:
        versionB_errors_per_minute_per_code[minute][code] += 1

sorted_codes = sorted(distinct_codes)
sorted_minutes = sorted(distinct_minutes)

codes_file = open('./static/all_codes.csv', 'w')
codes_csv_writer = csv.writer(codes_file)
codes_csv_writer.writerow(['code'])
for code in sorted_codes:
    codes_csv_writer.writerow([code])
    w = open('./static/data/{}.csv'.format(code), 'w')
    csv_writer = csv.writer(w)
    csv_writer.writerow(['time', version_a, version_b])
    for minute in sorted_minutes:
        csv_writer.writerow([
            minute,
            versionA_errors_per_minute_per_code[minute][code],
            versionB_errors_per_minute_per_code[minute][code]
        ])
    w.close()
codes_file.close()
