#!/bin/sh -xe

if [[ $# -ne 1 ]]; then
    echo "USAGE: ./run.sh /path/to/ErrorEventSet.csv"
    exit 2
fi


# Clear previous run output
rm -rf outputs/
rm -rf static && mkdir -p static/data

# compile and run job
mvn compile exec:exec -DinputPath="$1"

# copy html/js to static dir to serve
cp src/main/js/index.* static

# Add header to CSV and concat output
echo "start,end,error_code" > static/data/errors.csv
find ./outputs -type f -iname "*part*" | xargs cat >> static/data/errors.csv

# generate static data files for this set
cat $1 | ./bin/generate_chart_csvs.py

# start a static server to view results
(cd static && python3 -m http.server)