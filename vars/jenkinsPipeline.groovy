// vars/jenkinsPipeline.groovy
	
def common = new petprojects.common()

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

	def portsMap = [:]
	portsMap['toggling-it'] = 50010
	portsMap['micro-transactions-mgt-api'] = 50020
	portsMap['micro-transactions-read-api'] = 50030
	
    // now build, based on the configuration provided
    node {		
		try {
			currentBuild.result = "SUCCESS"
			
			def latestVersionPrefix = config.releaseVersion
			def featureVersionPrefix = '0.1.0'		
					
			def version = VersionNumber(versionNumberString: '.${BUILD_DATE_FORMATTED,\"yy\"}${BUILD_MONTH, XX}.${BUILDS_THIS_MONTH}')
			
			if (env.BRANCH_NAME != 'master') {
				if (common.isPRMergeBuild()) {
					version = latestVersionPrefix + version + '-beta'
				}
				else {
					version = featureVersionPrefix + version + '-alpha'
				}
				currentBuild.displayName = '#'+version			
				deleteDir()
				withEnv(['PIPELINE_VERSION='+version,'IMAGE_NAME='+config.imageName,'CONTAINER_NAME='+config.containerName,'OUTPUT_PATH=build','SLN_FILE='+config.slnFile, 'ASPNETCORE_VERSION='+config.aspnetcoreVersion, 'DOTNET_SDK_VERSION='+config.dotnetSdkVersion, 'PORT='+portsMap[config.imageName]]) {
					timestamps {
						common.checkout()
						common.prepareScripts()
						buildAspNetCore()
						common.unitTests()
						integrationTests()
						//if (!isPRMergeBuild()) {
						//	manualPromotion()
						//	deploy(config.imageName)
						//}
					}
				}
			} // master branch / production
			else {
				version = latestVersionPrefix + version
				currentBuild.displayName = '#'+version			
				deleteDir()
				withEnv(['PIPELINE_VERSION='+version,'IMAGE_NAME='+config.imageName,'CONTAINER_NAME='+config.containerName,'OUTPUT_PATH=build','SLN_FILE='+config.slnFile, 'ASPNETCORE_VERSION='+config.aspnetcoreVersion, 'DOTNET_SDK_VERSION='+config.dotnetSdkVersion, 'PORT='+portsMap[config.imageName]]) {
					timestamps {
						common.checkout()
						common.prepareScripts()			
						buildAspNetCore()
						common.unitTests()
						integrationTests()					
						deploy(config.imageName)
					}
				}
			}
		}
		catch (err) {
			currentBuild.result = "FAILURE"
			throw err
		}
	}
}

def buildAspNetCore(){
	common.buildStep('Build'){
		try {
			sh '''sh build.ci.sh;'''
		}
		finally {
			sh '''sh build.ci.cleanup.sh;'''						
		}
	}
}

def integrationTests(){
	common.buildStep('Integration Tests'){
		withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_USER_PASSWORD', usernameVariable: 'DOCKER_USER_NAME')]) {
			withEnv(['BUILD_VERSION='+env.PIPELINE_VERSION]) {
				sshagent(['Toggling-It-Api']) {
					try {						
						sh '''docker-compose -f docker-compose.release.yml up -d'''
							
						sh '''echo "containername=${CONTAINER_NAME}";
							sh build.ci.integrationtests.sh;'''
							
						sh '''echo "integration tests successful... pushing img to dockerhub...";
							docker login -u ${DOCKER_USER_NAME} -p ${DOCKER_USER_PASSWORD};
							sh build.ci.pushimg.sh;						
							docker logout;'''
						
						sh '''git tag -f ${PIPELINE_VERSION};
							git push origin ${PIPELINE_VERSION};'''								
						
						step([$class: 'MSTestPublisher', testResultsFile: '**/test/integration/**/*.trx', failOnError: true, keepLongStdio: true])
						
					} finally {				
						sh '''sh build.ci.integrationtests.cleanup.sh;'''
					}
				}
			}			
		}			
	}
}

def ensureServiceIsRunning(String imageName){
	sh '''echo "Ensuring Service is Running - ${IMAGE_NAME}";'''
	sh '''sh ensure-service-running.sh;'''
}

def updateRunningService(String imageName){
	sh '''echo "Updating Service - $IMAGE_NAME";'''
	sh '''sh update-service.sh;'''
}

def deploy(String imageName){
	common.buildStep('Deploy'){
		withCredentials([usernamePassword(credentialsId: 'sshrenatorenabee', passwordVariable: 'SSH_USER_PASSWORD', usernameVariable: 'SSH_USER_NAME')]) {
			ensureServiceIsRunning(imageName)
			updateRunningService(imageName)
		}
	}
}

def manualPromotion() {
	stage('Manual Promotion'){
		// we need a first milestone step so that all jobs entering this stage are tracked an can be aborted if needed
		milestone 1
		
		// time out manual approval after ten minutes
		timeout(time: 2, unit: 'MINUTES') {
			input message: "Deploy non-master build to production?"
		}
		
		// this will kill any job which is still in the input step
		milestone 2
	}
}