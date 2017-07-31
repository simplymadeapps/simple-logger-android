# simple-logger-android
A simple library for Android that stores logs and uploads them as text files to Amazon S3.
# Getting Started
// To do
# Usage
To record a log:
```javascript
SimpleAmazonLogs logger = new SimpleAmazonLogs(getApplication());
logger.addLog("Your log entry goes here.");
```
To upload logs:
```javascript
SimpleAmazonLogs logger = new SimpleAmazonLogs(getApplication());
logger.uploadLogsToAmazon(access_token, secret, bucket, directory, new SimpleAmazonLogCallback() { } );
```
# Important
1)  You must provide your own Amazon S3 credentials.  You must have your own access token, secret, and bucket for uploading.
2)  Recorded logs are automatically removed from the device after 7 days.  Make sure to consistently upload logs at least once every 7 days.
# Example Output
After a successful upload, you can find the text files in the specified location on Amazon S3.
Example files:
```javascript
2017-06-26.txt
2017-06-27.txt
2017-06-28.txt
```
Within the text file you will find all of the logs recorded on that day.  The timestamp of the record is automatically recorded.
Example output:
```javascript
2017-07-31 11:33:10 CDT - This is my first recorded log
2017-07-31 11:33:14 CDT - This is my second recorded log
2017-07-31 11:33:16 CDT - This is my third recorded log
```