import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription
import com.serena.air.beanstalk.BeanstalkHelper

//sdk imports
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest
import com.amazonaws.services.elasticbeanstalk.model.S3Location
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest

String accessKeyId = ""
String secretKey = ""
String region = "EU_WEST_2"
String s3BucketName = "elasticbeanstalk-eu-west-1-941683627343"
String s3KeyPrefix = "upload/"
String s3VersionLabel = "1.0.4"
File deployFile = new File("C:\\Temp\\myapp.zip")
String appName = "TestApp"
def envName = "Testapp-env"

BeanstalkHelper helper = new BeanstalkHelper(accessKeyId, secretKey, region)
List<EnvironmentDescription> environments = helper.getAWSBeanstalkClient().describeEnvironments(
        new DescribeEnvironmentsRequest()
).getEnvironments()
println environments
println helper.applicationEnvironmentExists("java-web-app", "Test")
println helper.applicationEnvironmentExists("e-9mtqvxvdwa")
helper.restartAppServer("e-9mtqvxvdwa")
System.exit(0)
/*
def solutionStacks =  helper.listSolutionStacks()

Collection<ConfigurationOptionSetting> configurationOptionSettings = new HashSet<>();
configurationOptionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:launchconfiguration", "InstanceType", "t2.medium"))

helper.createApplication("test2")
helper.createApplicationEnvironment("test2", "test2-env1", solutionStacks.get(0).toString(), configurationOptionSettings)

System.exit 0
*/

String s3ObjectId = helper.s3UploadVersionFile(s3BucketName, s3KeyPrefix, s3VersionLabel, deployFile)

if (!helper.applicationExists(appName)) {
    throw new RuntimeException("Application \"${appName}\" does not exist")
}
if (helper.applicationVersionExists(appName, s3VersionLabel)) {
    throw new RuntimeException("Version \"${s3VersionLabel}\" in application \"${appName}\" already exists")
}
helper.createApplicationVersion(appName, s3VersionLabel, s3BucketName, s3ObjectId)

if (!helper.applicationEnvironmentExists(appName, envName)) {
    throw new RuntimeException("Environment \"${envName}\" in application \"${appName}\" does not exist")
}
helper.updateEnvironmentWithVersion(appName, envName, s3VersionLabel)

helper.waitForEnvironmentStatusAndHealth(envName, "Ready", "Green")

println "Final status = " +  helper.getEnvironmentStatusAndHealth("Testapp-env")

