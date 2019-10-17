package com.serena.air.beanstalk

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions

//sdk imports

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.*
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest

class BeanstalkHelper {

    String accessKeyId
    String secretKey
    AWSCredentials awsCredentials
    AWSElasticBeanstalk awsBeanstalkClient
    Region awsRegion
    AmazonS3Client s3Client

    final static Long DELAY_INTERVAL = 1000 * 30
    final static Integer MAX_TRIES = 100

    BeanstalkHelper(accessKeyId, secretKey, region) {
        this.accessKeyId = accessKeyId
        this.secretKey = secretKey
        awsCredentials = new BasicAWSCredentials(String.valueOf(accessKeyId),
                String.valueOf(secretKey))
        awsBeanstalkClient = new AWSElasticBeanstalkClient(awsCredentials)
        awsRegion = Region.getRegion(Enum.valueOf(Regions.class, String.valueOf(region)))
        awsBeanstalkClient.setRegion(awsRegion)
        s3Client = new AmazonS3Client(awsCredentials)
        s3Client.setRegion(awsRegion)
    }

    AWSCredentials getAWSCredentials() { return awsCredentials }

    AWSElasticBeanstalk getAWSBeanstalkClient() { return awsBeanstalkClient }

    Region getAWSRegion() { return awsRegion }

    def s3GetVersionObject(String s3BucketName, String s3KeyPrefix, String versionLabel, File deployFile) {
        if (!s3Client.doesBucketExistV2(s3BucketName)) {
            throw new RuntimeException("Error: S3 Bucket \"${s3BucketName}\" does not exist in region \"${getAWSRegion().getName()}\"")
        } else {
            log("Using S3 bucket: ${s3BucketName}")
        }

        // remove fist character if file separator
        int s3KeyPrefixLength = s3KeyPrefix.length()
        char firstChar = s3KeyPrefix.charAt(0)
        if (firstChar == '/' || firstChar == '\\') {
            s3KeyPrefix = s3KeyPrefix.substring(1, s3KeyPrefixLength)
        }
        // remove last character if file separator
        s3KeyPrefixLength = s3KeyPrefix.length()
        char lastChar = s3KeyPrefix.charAt(s3KeyPrefixLength - 1)
        if (lastChar == '/' || lastChar == '\\') {
            s3KeyPrefix = s3KeyPrefix.substring(0, s3KeyPrefixLength - 1)
        }

        String s3ObjectId
        if (s3KeyPrefix) {
            s3ObjectId = s3KeyPrefix + "/" + versionLabel + "-" + deployFile.getName()
        } else {
            s3ObjectId = versionLabel + "-" + deployFile.getName()
        }

        if (!s3Client.doesObjectExist(s3BucketName, s3ObjectId)) {
            throw new RuntimeException("S3 Object \"${s3ObjectId}\" in bucket \"${s3BucketName}\" in region \"${getAWSRegion().getName()}\" does not exist ...")
        }

        return s3ObjectId
    }

    def s3UploadVersionFile(String s3BucketName, String s3KeyPrefix, String versionLabel, File deployFile) {
        if (!s3Client.doesBucketExistV2(s3BucketName)) {
            throw new RuntimeException("Error: S3 Bucket \"${s3BucketName}\" does not exist in region \"${getAWSRegion().getName()}\"")
        } else {
            log("Using S3 bucket: ${s3BucketName}")
        }

        if (!deployFile.exists()) {
            throw new RuntimeException("Error: File \"${deployFile.canonicalPath}\" does not exist")
        }

        // remove fist character if file separator
        int s3KeyPrefixLength = s3KeyPrefix.length()
        char firstChar = s3KeyPrefix.charAt(0)
        if (firstChar == '/' || firstChar == '\\') {
            s3KeyPrefix = s3KeyPrefix.substring(1, s3KeyPrefixLength)
        }
        // remove last character if file separator
        s3KeyPrefixLength = s3KeyPrefix.length()
        char lastChar = s3KeyPrefix.charAt(s3KeyPrefixLength - 1)
        if (lastChar == '/' || lastChar == '\\') {
            s3KeyPrefix = s3KeyPrefix.substring(0, s3KeyPrefixLength - 1)
        }

        String s3ObjectId
        if (s3KeyPrefix) {
            s3ObjectId = s3KeyPrefix + "/" + versionLabel + "-" + deployFile.getName()
        } else {
            s3ObjectId = versionLabel + "-" + deployFile.getName()
        }

        if (s3Client.doesObjectExist(s3BucketName, s3ObjectId)) {
            log("S3 Object \"${s3ObjectId}\" in bucket \"${s3BucketName}\" already exists in region \"${getAWSRegion().getName()}\", deleting it ...")
            s3Client.deleteObject(s3BucketName, s3ObjectId)
        }

        log("Uploading \"${deployFile.canonicalPath}\" to S3 storage as \"${s3ObjectId}\" ...")
        PutObjectRequest poRequest = new PutObjectRequest(s3BucketName, s3ObjectId, deployFile)
        s3Client.putObject(poRequest)
        log("Uploaded successfully")
        return s3ObjectId
    }

    def createApplication(String appName) {
        log("Creating new application \"${appName}\" ...")
        CreateApplicationRequest caRequest = new CreateApplicationRequest(appName)
                .withDescription("Created by Deployment Automation")
        def application = getAWSBeanstalkClient().createApplication(caRequest)
        return application.getApplication().getApplicationArn()
    }

    def deleteApplication(String appName, Boolean terminateEnvByForce) {
        log("Deleting application \"${appName}\" ...")
        DeleteApplicationRequest daRequest = new DeleteApplicationRequest()
                .withApplicationName(appName)
                .withTerminateEnvByForce(terminateEnvByForce)
        getAWSBeanstalkClient().deleteApplication(daRequest)
    }

    def createApplicationEnvironment(String appName, String envName, String cNAMEPrefix, String stackName,
                                     HashSet<ConfigurationOptionSetting> configOptions) {
        log("Creating new environment \"${envName}\" for application \"${appName}\" ...")
        CreateEnvironmentRequest ceRequest = new CreateEnvironmentRequest(appName, envName)
                .withCNAMEPrefix(cNAMEPrefix)
                .withSolutionStackName(stackName)
                .withDescription("Created by Deployment Automation").withPlatformArn()
        if (configOptions != null) {
            ceRequest.withOptionSettings(configOptions)
        }
        def environment = getAWSBeanstalkClient().createEnvironment(ceRequest)
        return environment.getEnvironmentId()
    }

    def terminateApplicationEnvironment(String envName, Boolean terminateResources, Boolean forceTerminate) {
        log("Terminating environment \"${envName}\" ...")
        TerminateEnvironmentRequest teRequest = new TerminateEnvironmentRequest()
                .withEnvironmentName(envName)
                .withTerminateResources(terminateResources)
                .withForceTerminate(forceTerminate)
        getAWSBeanstalkClient().terminateEnvironment(teRequest)
    }

    def updateApplicationEnvironment(String appName, String envName, String stackName, HashSet<ConfigurationOptionSetting> addConfigOptions, HashSet<OptionSpecification> removeOptionSpecifications) {
        log("Updating environment \"${envName}\" for application \"${appName}\" ...")
        UpdateEnvironmentRequest ueRequest = new UpdateEnvironmentRequest()
                .withApplicationName(appName)
                .withEnvironmentName(envName)
                .withDescription("Updated by Deployment Automation")
        if (stackName) {
            ueRequest.withSolutionStackName(stackName)
        }
        if (addConfigOptions != null) {
            ueRequest.withOptionSettings(addConfigOptions)
        }
        if (removeOptionSpecifications) {
            ueRequest.withOptionsToRemove(removeOptionSpecifications)
        }
        return getAWSBeanstalkClient().updateEnvironment(ueRequest)
    }

    def rebuildApplicationEnvironment(String envName) {
        log("Rebuilding environment \"${envName}\"  ...")
        RebuildEnvironmentRequest re = new RebuildEnvironmentRequest()
                .withEnvironmentName(envName)
        getAWSBeanstalkClient().rebuildEnvironment(re)
    }

    def restartAppServer(String envName) {
        log("Restarting app server for environment \"${envName}\"  ...")
        RestartAppServerRequest ra = new RestartAppServerRequest()
                .withEnvironmentName(envName)
        getAWSBeanstalkClient().restartAppServer(ra)
    }

    def swapEnvironmentCNAMEs(String sourceEnv, String targetEnv) {
        log("Swapping URLs for environment \"${sourceEnv}\"  with environment \"${targetEnv} ...")
        SwapEnvironmentCNAMEsRequest se = new SwapEnvironmentCNAMEsRequest()
                .withSourceEnvironmentName(sourceEnv)
                .withDestinationEnvironmentName(targetEnv)
        getAWSBeanstalkClient().swapEnvironmentCNAMEs(se)
    }

    def listSolutionStacks() {
        log("Listing solution stacks ...")
        ListAvailableSolutionStacksResult response = getAWSBeanstalkClient().listAvailableSolutionStacks()
        return response.getSolutionStacks()
    }

    def applicationExists(String appName) {
        List<ApplicationDescription> applications = awsBeanstalkClient.describeApplications(
                new DescribeApplicationsRequest().withApplicationNames(appName)
        ).getApplications()
        if (applications.size().toInteger() != 0) {
            return true
        } else {
            return false
        }
    }

    def applicationVersionExists(String appName, String versionLabel) {
        List<ApplicationVersionDescription> versions = awsBeanstalkClient.describeApplicationVersions(
                new DescribeApplicationVersionsRequest().withApplicationName(appName).withVersionLabels(versionLabel)
        ).getApplicationVersions()
        if (versions.size().toInteger() != 0) {
            return true
        } else {
            return false
        }
    }


    def applicationEnvironmentExists(String appName, String envName) {
        List<EnvironmentDescription> environments = awsBeanstalkClient.describeEnvironments(
                new DescribeEnvironmentsRequest().withApplicationName(appName).withEnvironmentNames(envName)
        ).getEnvironments()
        if (environments.size().toInteger() != 0) {
            return true
        } else {
            return false
        }
    }

    def applicationEnvironmentExists(String envName) {
        List<EnvironmentDescription> environments = awsBeanstalkClient.describeEnvironments(
                new DescribeEnvironmentsRequest().withEnvironmentNames(envName)
        ).getEnvironments()
        if (environments.size().toInteger() != 0) {
            return true
        } else {
            return false
        }
    }

    def createApplicationVersion(String appName, String versionLabel, s3BucketName, s3ObjectId) {
        log("Creating new application version \"${versionLabel}\" for application \"${appName}\" ...")
        CreateApplicationVersionRequest cavRequest = new CreateApplicationVersionRequest(appName, versionLabel)
                .withAutoCreateApplication(false)
                .withSourceBundle(new S3Location(s3BucketName, s3ObjectId))
                .withDescription("Uploaded by Deployment Automation")
        return getAWSBeanstalkClient().createApplicationVersion(cavRequest)
    }

    def deleteApplicationVersion(String appName, String versionLabel) {
        log("Updating application version \"${versionLabel}\" for application \"${appName}\" ...")
        DeleteApplicationVersionRequest davRequest = new DeleteApplicationVersionRequest(appName, versionLabel)
        return getAWSBeanstalkClient().deleteApplicationVersion(davRequest)
    }

    def updateEnvironmentWithVersion(String appName, String envName, String versionLabel) {
        log("Deploying application version \"${versionLabel}\" into environment \"${envName}\" ...")
        UpdateEnvironmentRequest ueRequest = new UpdateEnvironmentRequest()
                .withApplicationName(appName)
                .withEnvironmentName(envName)
                .withVersionLabel(versionLabel)
                .withDescription("Deployed by Deployment Automation")
        return getAWSBeanstalkClient().updateEnvironment(ueRequest)
    }

    def getEnvironmentStatusAndHealth(String envName) {
        List<EnvironmentDescription> environments = awsBeanstalkClient.describeEnvironments(
                new DescribeEnvironmentsRequest().withEnvironmentNames(envName)
        ).getEnvironments()
        if (environments.size().toInteger() == 0) {
            throw new RuntimeException("No environments with the name \"${envName}\" found")
        }

        EnvironmentDescription envDesc = environments.get(0)
        return envDesc.getHealth() + "/" + envDesc.getStatus()
    }

    def waitForEnvironmentStatusAndHealth(String envName, String envStatus, String envHealth) {
        log("Waiting for environment \"${envName}\" to transition to \"${envStatus}\" ...")

        int count = 0
        while (true) {
            if (count++ > MAX_TRIES) {
                throw new RuntimeException("Environment \"" + envName + "\" never transitioned to Ready")
            }

            List<EnvironmentDescription> environments = awsBeanstalkClient.describeEnvironments(
                    new DescribeEnvironmentsRequest()
                            .withEnvironmentNames(String.valueOf(envName))
            ).getEnvironments()

            if (environments.size().toInteger() == 0) {
                throw new RuntimeException("No environments with the name \"${envName}\" found")
            }

            EnvironmentDescription envDesc = environments.get(0)
            String curStatus = envDesc.getStatus()
            String curHealth = envDesc.getHealth()
            log("Status: ${curHealth} / ${curStatus}")
            if (curStatus.equalsIgnoreCase(String.valueOf(envStatus))) {
                if (envHealth == null || (envHealth != null && curHealth.equalsIgnoreCase(String.valueOf(envHealth)))) {
                    return
                }
            }
            Thread.sleep(DELAY_INTERVAL)
        }
    }

    static log(def message) {
        def date = new java.text.SimpleDateFormat("h:mma")
        def time = date.format(new Date()).toLowerCase()
        println "${time}  ${message}"
    }

}
