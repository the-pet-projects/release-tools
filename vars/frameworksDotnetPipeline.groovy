// vars/frameworksDotnetPipeline.groovy
	
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	common = new petprojects.common()
	
    // now build, based on the configuration provided
    node {		
		try {
			currentBuild.result = "SUCCESS"
			
			def latestVersionPrefix = config.releaseVersion
			def featureVersionPrefix = '0.1'		
					
			def version = VersionNumber(versionNumberString: '.${BUILD_DATE_FORMATTED,\"yyDDD\"}.${BUILDS_TODAY}')
			
			if (env.BRANCH_NAME != 'master') {
				if (common.isPRMergeBuild()) {
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
					common.checkout()
					common.prepareScripts()
					buildFramework()
					common.unitTests()
					pushPackages()
					common.tagCommit()
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
	common.buildStep('Push Packages'){
		try {
			withCredentials([string(credentialsId: 'nugetorg-api-key', variable: 'NUGET_API_KEY')]) {
				sh '''sh build.ci.pushpackages.sh;'''
			}
		}
		finally {
			sh '''sh build.ci.pushpackages.cleanup.sh;'''		
		}
	}
}

def buildFramework(){
	common.buildStep('Build'){
		try {
			sh '''sh build.ci.frameworksdotnet.sh;'''
		}
		finally {
			sh '''sh build.ci.frameworksdotnet.cleanup.sh;'''		
		}
	}
}