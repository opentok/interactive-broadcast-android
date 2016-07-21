# Interactive Broadcast: Archiving

This document describes how to set up archiving on an Interactive Broadcast (IB) instance.

## Create an Amazon S3 bucket

Create your AWS bucket using the instructions available <a href="http://docs.aws.amazon.com/AmazonS3/latest/gsg/CreatingABucket.html">here</a>.

Set your _CORS configuration_ as follows:

```xml

<?xml version="1.0" encoding="UTF-8"?>
<CORSConfiguration xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
  <CORSRule>
    <AllowedOrigin>*</AllowedOrigin>
    <AllowedMethod>GET</AllowedMethod>
    <AllowedMethod>POST</AllowedMethod>
    <AllowedMethod>PUT</AllowedMethod>
    <AllowedHeader>*</AllowedHeader>
  </CORSRule>
</CORSConfiguration>
```

Inside your bucket, create a folder using your OpenTok API key as the name and make it public.

Get your Security Credentials:

1. Open the IAM console
2. In the navigation pane, select **Users**.
3. Select your IAM user name (not the check box).
4. Click the **Security Credentials** tab and select **Create Access Key**. You can view the access key by selecting **Show User Security Credentials**. Here is an example of how your credentials should appear:

```
Access Key ID: AKIAIOSFODNN7EXAMPLE
Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

Choose **Download Credentials**, and store the keys in a secure location.

Your secret key will no longer be available through the AWS Management Console; you will have the only copy. Keep it confidential in order to protect your account, and never email it. Do not share it outside your organization, even if an inquiry appears to come from AWS or Amazon.com. No one who legitimately represents Amazon will ever ask you for your secret key.


## Set up the environment variables in Heroku

You can get your Amazon S3 access key ID, secret access key, and bucket name in your Amazon S3 console:

1. Login into heroku using your credentials.
2. Click on your heroku app, open the **Settings** tab, and select **Reveal config vars**.
3. Create or update the following environment variables:<br/>

| Variable        | Description  |
| ------------- | ------------- |
| `AWS_ACCESS_KEY_ID`   | S3 access key ID.   |
| `AWS_SECRET_ACCESS_KEY`  | S3 secret access key. |
| `S3_BUCKET_NAME`   | The bucket name. |
| `S3_COMPOSEDVIDEO_URL`   | The first part of the URL required to retrieve the file in your bucket. Use this format: `https//s3.amazonaws.com/[S3_BUCKET_NAME]`. For example, `https://s3.amazonaws.com/spotlight-tokbox/`. |



## Set up your AWS S3 bucket in Tokbox

1. Login into Tokbox.
2. Open your project.
3. Scroll down to the **Archiving** section, and click **Set up your cloud storage now**.
4. Click on the Amazon S3 logo.
5. Enter the following S3 data: access key, secret access key and bucket name.
