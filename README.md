## Error Rate Detection

We want to compare the error rates for each error code across two different deployments of the same application.
The inputs in `inputs/` have two sets of sample data that have error events by error code, version and timestmap.

We're using a local Apache Flink execution environment to compare error rates per code across a sliding window.
If for a given time window we see a 200% increase in error rate with the same error code, we assume there has been a
deploy regression and log the bounds of that window.

The output of this job is then charted per error code using d3.js, windows with regressions are highlighted in red.

The results of this job using ErrorStreamSetA are hosted at:
https://chrismcbride.github.io/errordetection-results/

For example, for error code 0 we detected no regressions (orange line is new version, blue old):

![error0](https://chrismcbride.github.io/errordetection-results/screenshots/no-error.png)

For error code 6 we detected two windows that may indicate a regression (shaded red). This may be overly aggressive
depending on the application, we can tweak the size of our sliding window to be larger to collect a larger sample
before "rolling back". The risk there it would take longer for us to react to a critical situation.

![error6](https://chrismcbride.github.io/errordetection-results/screenshots/error6.png)

For error code 4 we saw generally elevated error rates throughout the sample. The darker shades of red mean there are
multiple overlapping windows flagged as errant.

![error4](https://chrismcbride.github.io/errordetection-results/screenshots/error4.png)


## Requirements

* JDK 8 or 11
* Maven (tested with v3.6)
* Python3

OR

* Docker

## Running Locally

`./bin/run.sh inputs/ErrorStreamSetA.csv`

This command will:
* Compile and run the flink job
* Copy output data to the `static/` directory
* Start a webserver on port 8000 to view the results

Alternatively, you can use docker:

```
docker build . -t netflix
docker run --rm -it -p8000:8000 netflix inputs/ErrorStreamSetA.csv
```

## Releases

A fat jar is provided as a github release:

```
curl -L https://github.com/chrismcbride/errordetection/releases/download/v0.1/errordetector.tgz | tar -xzv --
java -jar target/errordetection-1.0-SNAPSHOT.jar ~/path/to/SampleData.csv
```
