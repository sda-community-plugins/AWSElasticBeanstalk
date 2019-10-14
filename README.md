# AWS Elastic Beanstalk plugin

The _AWS Elastic Beanstalk_ plugin is a Cloud Deployment plugin. It is run during the development and deployment process 
to manage and deploy web applications into [AWS Elastic Beanstalk](https://aws.amazon.com/elasticbeanstalk/). It interacts 
directly with the Elastic Beanstalk SDK.

This plugin is a work in progress but it is intended to provide the following steps:

* [x] **Application Exists** - Check whether the specified Application already exists.
* [x] **Create Application** - Create a new Elastic Beanstalk Application.  
* [ ] **Delete Application** - TBD
* [x] **List Stacks** - List all of the Solution Stack Names for an Elastic Beanstalk Environment and allow the user to select one.  
* [x] **Environment Exists** - Check whether the specified Environment already exists in the Application.  
* [x] **Create Environment** - Create a new Elastic Beanstalk Environment.  
* [ ] **Terminate Environment** - TBD
* [ ] **Clone Environment** - TBD
* [ ] **Swap Environment URLs** - TBD
* [ ] **Save Configuration** - TBD
* [ ] **Load Configuration** - TBD
* [ ] **Restart App Server(s)** - TBD
* [ ] **Rebuild Environment** - TBD
* [x] **Deploy into Elastic Beanstalk** - Deploy an application into Elastic Beanstalk.  
* [x] **Application Version Exists** - Check whether the specified Application Version already exists.   
* [ ] **Delete Application Version** - TBD
* [x] **Wait for Environment** - Wait for an Elastic Beanstalk environment to transition to the specified Health and Status.  
* [x] **Environment Health and Status** - Get the Health and Status of an Elastic Beanstalk environment and set the properties "envHealth" and "envStatus" accordingly.                                          
                                        
### Installing the plugin
 
Download the latest version from the _release_ directory and install into Deployment Automation from the 
**Administration\Automation\Plugins** page.

### Using the plugin

The main step of the plugin is **Deploy into Elastic Beanstalk**  - this step works by uploading a web application 
(e.g. Java WAR file) into an S3 bucket and creating a new version of the Elastic Beanstalk application using this. 
It then deploys this version into the environment you specify. You can either create the Beanstalk application and
environment from Amazon's web console or by using the steps in this plugin.
You will need to know the name of an S3 bucket to upload to - this can be found from Amazon's S3 console.

You will also need to create two Deployment Automation 
[System Properties](http://help.serena.com/doc_center/sra/ver6_3/sda_help/sra_adm_sys_properties.html)
called `EC2AccessKeyId` that refers to your AWS Access Key and `EC2SecretKey` that refers to the Access Key's Secret.
 
See the [examples](examples) directory for some example processes.
         
### Building the plugin

To build the plugin you will need to clone the following repositories (at the same level as this repository):

 - [mavenBuildConfig](https://github.com/sda-community-plugins/mavenBuildConfig)
 - [plugins-build-parent](https://github.com/sda-community-plugins/plugins-build-parent)
 - [air-plugin-build-script](https://github.com/sda-community-plugins/air-plugin-build-script)
 
 and then compile using the following command
 ```
   mvn clean package
 ```  

This will create a _.zip_ file in the `target` directory when you can then install into Deployment Automation
from the **Administration\Automation\Plugins** page.

If you have any feedback or suggestions on this template then please contact me using the details below.

Kevin A. Lee

kevin.lee@microfocus.com

**Please note: this plugins is provided as a "community" plugin and is not supported by Micro Focus in any way**.
