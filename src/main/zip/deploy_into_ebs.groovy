// --------------------------------------------------------------------------------
// Deploy an application into Elastic Beanstalk.
// --------------------------------------------------------------------------------

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool

import com.serena.air.beanstalk.BeanstalkHelper

//
// Create some variables that we can use throughout the plugin step.
// These are mainly for checking what operating system we are running on.
//
final def PLUGIN_HOME = System.getenv()['PLUGIN_HOME']
final String lineSep = System.getProperty('line.separator')
final String osName = System.getProperty('os.name').toLowerCase(Locale.US)
final String pathSep = System.getProperty('path.separator')
final boolean windows = (osName =~ /windows/)
final boolean vms = (osName =~ /vms/)
final boolean os9 = (osName =~ /mac/ && !osName.endsWith('x'))
final boolean unix = (pathSep == ':' && !vms && !os9)

//
// Initialise the plugin tool and retrieve all the properties that were sent to the step.
//
final def  apTool = new AirPluginTool(this.args[0], this.args[1])
final def  props  = new StepPropertiesHelper(apTool.getStepProperties(), true)

//
// Set a variable for each of the plugin steps's inputs.
// We can check whether a required input is supplied (the helper will fire an exception if not) and
// if it is of the required type.
//
String accessKeyId = props.notNull('accessKeyId')
String secretKey = props.notNull('secretKey')
String region = props.notNull('region')
String appName = props.notNull('appName')
String envName = props.notNull('envName')
String s3BucketName = props.optional('s3BucketName')
String s3KeyPrefix = props.optional('s3KeyPrefix')
String versionLabel = props.notNull('versionLabel')
String baseDir = props.notNull('baseDir',)
String versionFile = props.notNull('versionFile')
Boolean checkForExisting = props.optionalBoolean('checkForExisting', true)
Boolean waitForEnvironment = props.optionalBoolean('waitForEnvironment', true)
Boolean debugMode = props.optionalBoolean("debugMode", false)
File workDir = new File(baseDir).canonicalFile
File deployFile = new File(workDir.canonicalPath + File.separatorChar + versionFile)

println "----------------------------------------"
println "-- STEP INPUTS"
println "----------------------------------------"

//
// Print out each of the property values.
//
println "Working directory: ${workDir.canonicalPath}"
println "Access Key Id: ${accessKeyId}"
String printedSecretKey = secretKey.replaceAll("(.*)", "\\*");
println "Secret Key: ${printedSecretKey}"
println "Region: ${region}"
println "Application Name: ${appName}"
println "Environment Name: ${envName}"
println "Application Version: ${versionLabel}"
println "Base Directory: ${baseDir}"
println "Version File: ${versionFile}"
println "S3 bucket Name: ${s3BucketName}"
println "S3 Key Prefix: ${s3KeyPrefix}"
println "Check for Existing Version: ${checkForExisting}"
println "Wait for environment: ${waitForEnvironment}"
println "Debug Output: ${debugMode}"
if (debugMode) { props.setDebugLoggingMode() }

println "----------------------------------------"
println "-- STEP EXECUTION"
println "----------------------------------------"

int exitCode = -1;

//
// The main body of the plugin step - wrap it in a try/catch statement for handling any exceptions.
//
try {

    if (!deployFile.exists()) {
        throw new IllegalArgumentException("Cannot find file: ${deployFile}.")
    }

    BeanstalkHelper helper = new BeanstalkHelper(accessKeyId, secretKey, region)
    helper.log("Using region \"${helper.getAWSRegion().getName()}\"")

    //
    // validation
    //

    // check application exists
    if (!helper.applicationExists(appName)) {
        throw new RuntimeException("Application \"${appName}\" does not exist")
    }
    // check environment exists
    if (!helper.applicationEnvironmentExists(appName, envName)) {
        throw new RuntimeException("Environment \"${envName}\" in application \"${appName}\" does not exist")
    }

    String s3ObjectId = null

    // should we use existing version (from previous deployment)
    if (checkForExisting) {
        // check if version already exists
        if (helper.applicationVersionExists(appName, versionLabel)) {
            helper.log("Using existing version \"${versionLabel}\" in application \"${appName}\"")
            s3ObjectId = helper.s3GetVersionObject(s3BucketName, s3KeyPrefix, versionLabel, deployFile)
        } else {
            helper.log("Using existing version selected but \"${versionLabel}\" in application \"${appName}\" does not exist, creating it ...")
            // upload file into S3 bucket
            s3ObjectId = helper.s3UploadVersionFile(s3BucketName, s3KeyPrefix, versionLabel, deployFile)
            // create new application version
            helper.createApplicationVersion(appName, versionLabel, s3BucketName, s3ObjectId)
        }
    } else {
        // check if version already exists
        if (helper.applicationVersionExists(appName, versionLabel)) {
            throw new RuntimeException("Version \"${versionLabel}\" in application \"${appName}\" already exists")
        }
        // upload file into S3 bucket
        s3ObjectId = helper.s3UploadVersionFile(s3BucketName, s3KeyPrefix, versionLabel, deployFile)
        // create new application version
        helper.createApplicationVersion(appName, versionLabel, s3BucketName, s3ObjectId)
    }

    // update environment with application version
    helper.updateEnvironmentWithVersion(appName, envName, versionLabel)
    
    // wait for deployment to complete
    if (waitForEnvironment) {
        helper.waitForEnvironmentStatusAndHealth(envName, "Ready", null)
    }

    String statusAndHealth = helper.getEnvironmentStatusAndHealth(envName)
    helper.log("Status of environment \"${envName}\" is: ${statusAndHealth}")

    println "----------------------------------------"
    println "-- STEP OUTPUTS"
    println "----------------------------------------"

    def (envStatus, envHealth) = statusAndHealth.split('/', 2)
    apTool.setOutputProperty("s3ObjectId", s3ObjectId)
    println("Setting \"s3ObjectId\" output property to \"${s3ObjectId}\"")
    apTool.setOutputProperty("envStatus", envStatus)
    println("Setting \"envStatus\" output property to \"${envStatus}\"")
    apTool.setOutputProperty("envHealth", envHealth)
    println("Setting \"envHealth\" output property to \"${envHealth}\"")

    apTool.setOutputProperties()

    exitCode = 0;

} catch (all) {
    println "ERROR: " + all.getMessage()
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode);
