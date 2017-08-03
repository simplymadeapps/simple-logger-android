# simple-logger-android
A simple library for Android that stores logs and uploads them as text files to Amazon S3.
# Purpose
The purpose of this project is to get logs from a specific user's device that can help when debugging.  If a user has a very specific issue and you are unable to reproduce it, just add some logging statements to get more information.  You can have them manually send the recorded logs or you can set it up to automatically upload to Amazon S3.
# Getting Started
// To do

Add permissions to AndroidManifest.xml:
```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
Initialize the logger - this should be called at the start of your application or from the launcher activity and must be called before calling any other functions:
```javascript
SimpleAmazonLogs.init(getApplication());
```
# Usage
To record a log:
```javascript
SimpleAmazonLogs.addLog("Your log entry goes here.");
```
To view all records:
```javascript
List<RecordedLog> logs = SimpleAmazonLogs.getAllLogs();
```
To upload logs:
```javascript
// You will need credentials to store to Amazon S3
SimpleAmazonLogs.setAmazonCredentials(access_token, secret_token, bucket, region);
// Upload to Amazon S3 into specified directory
SimpleAmazonLogs.uploadLogsToAmazon(directory, new SimpleAmazonLogCallback() {
	@Override
	public void onSuccess() {

	@Override
	public void onFailure(Exception e, int successful, int failures) {
	}
});
```
# Important
1)  You must provide your own Amazon S3 credentials.  You must have your own access token, secret, and bucket for uploading.

2)  Uploading to Amazon S3 is optional.  You are free to just store logs and retrieve the list at anytime.  Feel free to upload the list of logs to wherever else.

3)  Recorded logs are automatically removed from the device after 7 days.  Make sure to consistently upload logs at least once every 7 days if you do not want any logs to be lost.

4)  If you want logs to last longer than 7 days, use the method `logger.setStorageDuration(365)`.  This allows you to specify how many days the logs will be stored on the device.  Please note, if you lower this number (for example, from 7 days down to 3 days) recorded logs will be immediately trimmed to 3 days.
# Example Output
After a successful upload, you can find the text files in the specified location on Amazon S3.
Example files:
```
2017-06-26.txt
2017-06-27.txt
2017-06-28.txt
```
Within the text file you will find all of the logs recorded on that day.  The timestamp of the record is automatically recorded.
Example output:
```
2017-07-31 11:33:10 CDT - This is my first recorded log
2017-07-31 11:33:14 CDT - This is my second recorded log
2017-07-31 11:33:16 CDT - This is my third recorded log
```