# Amazon Elastic Beanstalk Example Application

This directory contains a Deployment Automation [Global Process](http://help.serena.com/doc_center/sra/ver6_3/sda_help/sra_stdalone_process.html) and sample Java WAR file
for deploying an application into Elastic Beanstalk

Files
-----
 - [beanstalk-test.json](beanstalk-test.json) - example global process
 - [java-web-app.war](java-web-app.war) - example WAR file to upload

To try this example out import the [Global Process](http://help.serena.com/doc_center/sra/ver6_3/sda_help/sra_stdalone_process.html) 
in Deployment Automation by navigating to **Management > Global Processes** and clicking on **Import**. Once import click on the **Details**
of the process and change **file.baseDir** to the directory where you downloaded `java-web-app.war`
and change **s3.bucket** to the name of your S3 bucket to upload to. When you run the process it will
create a new Beanstalk Application and Environment and Deploy the version. On subsequent runs
you can choose whether to re-use the Beanstalk Environment or create a new one.
