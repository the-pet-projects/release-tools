// vars/jenkinsPipeline.groovy
	
def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
	
    // now build, based on the configuration provided
    node {
		currentBuild.result = "SUCCESS"
		
		def latestVersionPrefix = config.releaseVersion
		def featureVersionPrefix = '0.1.0'		
				
		def version = VersionNumber(versionNumberString: '.${BUILD_DATE_FORMATTED,\"yy\"}${BUILD_MONTH, XX}.${BUILDS_THIS_MONTH}')
		
		try {
			if (env.BRANCH_NAME != 'master') {
				if (isPRMergeBuild()) {
					version = latestVersionPrefix + version + '-beta'
				}
				else {
					version = featureVersionPrefix + version + '-alpha'
				}
				withEnv(['PIPELINE_VERSION='+version]) {
					timestamps {
						checkout()					
						build()
						unitTests()
						integrationTests()
						if (!isPRMergeBuild()) {
							manualPromotion()
							deploy()
						}
					}
				}
			} // master branch / production
			else {
				version = latestVersionPrefix + version
				withEnv(['PIPELINE_VERSION='+version]) {
					timestamps {
						checkout()					
						build()
						unitTests()
						integrationTests()					
						deploy()
					}
				}
			}
		}
		catch (err) {
			currentBuild.result = "FAILURE"
			cleanWs()
			throw err
		}
	}
}

def gitCommit = ''

def checkout(){
	stage('Checkout'){
		deleteDir()
		checkout scm
		gitCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%H'").trim()
	}
}

def build(){
	buildStep('Build'){
		currentBuild.displayName = '#'+env.PIPELINE_VERSION
		try {
			sh '''sh ./deploy/scripts/build.ci.sh;'''
		}
		finally {
			sh '''sh ./deploy/scripts/build.ci.cleanup.sh;'''						
		}
	}
}

def unitTests(){
	buildStep('Unit Tests'){
		try {
			sh '''sh ./deploy/scripts/build.ci.unittests.sh;'''
			step([$class: 'MSTestPublisher', testResultsFile: '**/test/unit/**/*.trx', failOnError: true, keepLongStdio: true])
		}
		finally {
			sh '''sh ./deploy/scripts/build.ci.unittests.cleanup.sh;'''		
		}
	}
}

def integrationTests(){
	buildStep('Integration Tests'){
		withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_USER_PASSWORD', usernameVariable: 'DOCKER_USER_NAME')]) {
			sshagent(['Toggling-It-Api']) {
				sh '''BUILD_VERSION=${PIPELINE_VERSION};
					export BUILD_VERSION;
					sh ./deploy/scripts/build.ci.integrationtests.sh;
					exitCode=$?;
					if [ $exitCode -eq 0 ]; then
						echo "integration tests successful... pushing img to dockerhub...";
						docker login -u ${DOCKER_USER_NAME} -p ${DOCKER_USER_PASSWORD};
						sh ./deploy/scripts/build.ci.pushimg.sh;
						exitCode=$?;
						docker logout;
					fi;
					sh ./deploy/scripts/build.ci.integrationtests.cleanup.sh;
					exit $exitCode;'''
				
				sh '''git tag -f ${PIPELINE_VERSION};
					git push origin ${PIPELINE_VERSION};'''								
				
				step([$class: 'MSTestPublisher', testResultsFile: '**/test/integration/**/*.trx', failOnError: true, keepLongStdio: true])
			}
		}			
	}
}

def deploy(){
	stage('Deploy'){
		withCredentials([usernamePassword(credentialsId: 'sshrenatorenabee', passwordVariable: 'SSH_USER_PASSWORD', usernameVariable: 'SSH_USER_NAME')]) {
			executeSshCommand(env.SSH_USER_NAME, env.SSH_USER_PASSWORD, "docker service update -d=false --image petprojects/${config.imageName}:${env.PIPELINE_VERSION} ${config.imageName}")
		}
	}
}

def isPRMergeBuild() {
    return (env.BRANCH_NAME ==~ /^PR-\d+$/)
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

def gitRepoUrl() {
    def tokens = "${env.JOB_NAME}".tokenize('/')
	def org = tokens[tokens.size()-3]
	def repo = tokens[tokens.size()-2]
	def result = 'https://github.com/' + org + '/' + repo
	result
}

void buildStep(String context, Closure closure) {
	stage(context) {
		try {
			setBuildStatus(context, "In progress...", "PENDING");
			closure();
			setBuildStatus(context, "Success", "SUCCESS");
		} catch (Exception e) {
			setBuildStatus(context, e.toString().take(140), "FAILURE");
			throw e
		}
	}
}

// Updated to account for context
void setBuildStatus(String context, String message, String state) {
	step([
		$class: "GitHubCommitStatusSetter",
		commitShaSource: [$class: 'ManuallyEnteredShaSource', sha: gitCommit],
		reposSource: [$class: "ManuallyEnteredRepositorySource", url: gitRepoUrl()],
		contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
		errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
		statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
	]);
}