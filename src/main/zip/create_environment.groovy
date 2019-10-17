// --------------------------------------------------------------------------------
// Create a new Elastic Beanstalk Environment.
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
String cNAMEPrefix = props.optional('cNAMEPrefix').toLowerCase()
String stackName = props.notNull('stackName')
String configOptions = props.optional('configOptions')
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
println "Application: ${appName}"
println "Environment: ${envName}"
println "Domain: ${cNAMEPrefix}"
println "Solution Stack: ${stackName}"
println "Configuration Option Settings: ${configOptions}"
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

    //
    // validation
    //

    // check application exists
    if (!helper.applicationExists(appName)) {
        throw new RuntimeException("Application \"${appName}\" does not exist")
    }
    // check environment exists
    if (helper.applicationEnvironmentExists(appName, envName)) {
        throw new RuntimeException("Environment \"${envName}\" in application \"${appName}\" already exists")
    }

    Collection<ConfigurationOptionSetting> configurationOptionSettings = new HashSet<>();
    if (configOptions) {
        configOptions.eachLine {
            if (it && it.indexOf('->') > 0) {
                def (namespace, optName, optValue) = it.split('->')
                configurationOptionSettings.add(new ConfigurationOptionSetting(namespace, optName, optValue))
                if (debugMode) {
                    helper.log('Setting option: [' + namespace + '] to ' + optName + ':' + optValue)
                }
            }
            else if (it) {
                helper.log("Found invalid option setting $it - missing -> separator")
            }
        }
    }

    def envId = helper.createApplicationEnvironment(appName, envName, cNAMEPrefix, stackName, configurationOptionSettings)


    println "----------------------------------------"
    println "-- STEP OUTPUTS"
    println "----------------------------------------"

    apTool.setOutputProperty("envId", envId)
    println("Setting \"envId\" output property to \"${envId}\"")
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

