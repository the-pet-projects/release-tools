// vars/frameworksDotnetPipeline.groovy
	
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    // now build, based on the configuration provided
    node {		
		try {
			currentBuild.result = "SUCCESS"
			
			def latestVersionPrefix = config.releaseVersion
			def featureVersionPrefix = '0.1.0'		
					
			def version = VersionNumber(versionNumberString: '.${BUILD_DATE_FORMATTED,\"yy\"}${BUILD_MONTH, XX}.${BUILDS_THIS_MONTH}')
			
			if (env.BRANCH_NAME != 'master') {
				if (isPRMergeBuild()) {
					version = latestVersionPrefix + version + '-beta'
				}
				else {
					version = featureVersionPrefix + version + '-alpha'
				}
			} // master branch
			else {
				version = latestVersionPrefix + version
			}
			
			currentBuild.displayName = '#'+version			
			deleteDir()
			withEnv(['PIPELINE_VERSION='+version, 'SLN_FILE='+config.slnFile, 'DOTNET_SDK_VERSION='+config.dotnetSdkVersion]) {
				timestamps {
					checkout()
					prepareScripts()
					buildFramework()
					unitTests()
					pushPackages()
				}
			}
		}
		catch (err) {
			currentBuild.result = "FAILURE"
			throw err
		}
	}
}

def pushPackages(){
	buildStep('Unit Tests'){
		try {
			sh '''sh build.ci.pushpackages.sh;'''
		}
		finally {
			sh '''sh build.ci.pushpackages.cleanup.sh;'''		
		}
	}
}

def buildFramework(){
	buildStep('Build'){
		try {
			sh '''sh build.ci.frameworksdotnet.sh;'''
		}
		finally {
			sh '''sh build.ci.frameworksdotnet.cleanup.sh;'''		
		}
	}
}