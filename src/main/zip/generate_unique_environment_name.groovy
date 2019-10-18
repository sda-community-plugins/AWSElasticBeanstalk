// --------------------------------------------------------------------------------
// Generates a valid Elastic Beanstalk Environment name, checking if an environment of same
// name already exists and set the output property "envName" accordingly.
// --------------------------------------------------------------------------------

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool

import com.serena.air.beanstalk.BeanstalkHelper
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting

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
File workDir = new File('.').canonicalFile
String accessKeyId = props.notNull('accessKeyId')
String secretKey = props.notNull('secretKey')
String region = props.optional('region')
String defaultRegion = props.optional("defaultRegion")
String appName = props.notNull('appName')
String envName = props.notNull('envName')
Boolean debugMode = props.optionalBoolean("debugMode", false)
String ec2Region = (defaultRegion.isEmpty() ? region : defaultRegion)

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
println "Region: ${ec2Region}"
println "DA Application: ${appName}"
println "DA Environment: ${envName}"
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

    BeanstalkHelper helper = new BeanstalkHelper(accessKeyId, secretKey, ec2Region)
    helper.log("Using region \"${helper.getAWSRegion().getName()}\"")

    String generatedEnvName = new String("${appName}-${envName}").replaceAll("[^A-Za-z0-9-]", "");
    if (debugMode) {
        helper.log("Checking Beanstalk Environment name: \"${generatedEnvName}\"")
    }
    if (helper.applicationEnvironmentExists(appName, generatedEnvName)) {
        println "ERROR - Beanstalk Environment name \"${generatedEnvName}\" already exists"
        System.exit(1)
    }
    /*int index = 1;
    TODO: allow multiple environments to exist
    String tmpEnvName = generatedEnvName
    while (helper.applicationEnvironmentExists(appName, generatedEnvName)) {
        if (debugMode) {
            helper.log("Environment \"${generatedEnvName}\" already exists...")
        }
        generatedEnvName = new String("${tmpEnvName}-").concat(Integer.toString(index++))
        if (debugMode) {
            helper.log("Trying new name: \"${generatedEnvName}\"...")
        }
    }*/

    helper.log("Generated Beanstalk Environment name: \"${generatedEnvName}\"")

    println "----------------------------------------"
    println "-- STEP OUTPUTS"
    println "----------------------------------------"

    apTool.setOutputProperty("envName", generatedEnvName.trim())
    println("Setting \"envName\" output property to \"${generatedEnvName}\"")
    apTool.storeOutputProperties()

    exitCode = 0

} catch (all) {
    println "ERROR: " + all.getMessage()
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode);

