// --------------------------------------------------------------------------------
// TBD
// --------------------------------------------------------------------------------

import com.serena.air.StepFailedException
import com.serena.air.StepPropertiesHelper
import com.urbancode.air.AirPluginTool

import com.serena.air.beanstalk.BeanstalkHelper

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting
import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification

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
String region = props.notNull('region')
String appName = props.notNull('appName')
String envName = props.notNull('envName')
String stackName = props.notNull('stackName')
String addOptions = props.optional('addConfigOptions')
String removeOptions = props.optional('removeConfigOptions')
Boolean debugMode = props.optionalBoolean("debugMode", false)

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
println "Solution Stack Name: ${stackName}"
println "Add Configuration: ${addOptions}"
println "Remove Configuration: ${removeOptions}"
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

    Collection<ConfigurationOptionSetting> addConfigurationOptionSettings = new HashSet<>();
    if (addOptions) {
        addOptions.eachLine {
            if (it && it.indexOf('->') > 0) {
                def (namespace, optName, optValue) = it.split('->')
                addConfigurationOptionSettings.add(new ConfigurationOptionSetting(namespace, optName, optValue))
                println 'Setting option: [' + namespace + '] to ' + optName + ':' + optValue
            }
            else if (it) {
                println "Found invalid option setting $it - missing -> separator"
            }
        }
    }

    Collection<OptionSpecification> removeOptionSpecifications = new HashSet<>();
    if (removeOptions) {
        removeOptions.eachLine {
            if (it && it.indexOf('->') > 0) {
                def (namespace, optName) = it.split('->')
                removeOptionSpecifications.add(new OptionSpecification().withNamespace(namespace).withOptionName(optName))
                println 'Removing option: [' + namespace + '] ' + optName
            }
            else if (it) {
                println "Found invalid option setting $it - missing -> separator"
            }
        }
    }

    helper.updateApplicationEnvironment(appName, envName, stackName, addConfigurationOptionSettings, removeOptionSpecifications)

    exitCode = 0

} catch (all) {
    println "ERROR: " + all.getMessage()
    System.exit 1
}

//
// An exit with a zero value means the plugin step execution will be deemed successful.
//
System.exit(exitCode);
