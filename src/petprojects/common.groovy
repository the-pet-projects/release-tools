// src/petprojects/common.groovy

package petprojects;

def executeSshCommand(String username, String password, String cmd) {
	def sshCmd = "sshpass -p '${password}' ssh ${username}@10.0.1.5 -o StrictHostKeyChecking=no '${cmd}'"
	sh sshCmd
}

def gitCommit = ''

def checkout(){
	stage('Checkout'){
		checkout scm
		gitCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%H'").trim()
	}
}

def getSharedFile(String name){
	def file = libraryResource name
	writeFile file: name, text: file
}

def prepareScripts(){
	getSharedFile('build.ci.cleanup.sh')
	getSharedFile('build.ci.integrationtests.cleanup.sh')
	getSharedFile('build.ci.integrationtests.sh')
	getSharedFile('build.ci.pushimg.sh')
	getSharedFile('build.ci.sh')
	getSharedFile('build.ci.unittests.cleanup.sh')
	getSharedFile('build.ci.unittests.sh')
	getSharedFile('build.ci.frameworksdotnet.cleanup.sh')
	getSharedFile('build.ci.frameworksdotnet.sh')
	getSharedFile('build.ci.pushpackages.cleanup.sh')
	getSharedFile('build.ci.pushpackages.sh')
	getSharedFile('docker-compose.build.yml')
	getSharedFile('docker-compose.frameworksdotnet.yml')
	getSharedFile('docker-compose.integrationtests.yml')
	getSharedFile('docker-compose.unittests.yml')
	getSharedFile('docker-compose.pushpackages.yml')
	getSharedFile('run-integration-tests.sh')
	getSharedFile('run-unit-tests.sh')
	getSharedFile('pushpackages.sh')
	getSharedFile('ensure-service-running.sh')
	getSharedFile('update-service.sh')
}

def isPRMergeBuild() {
    return (env.BRANCH_NAME ==~ /^PR-\d+$/)
}

def unitTests(){
	buildStep('Unit Tests'){
		try {
			sh '''sh build.ci.unittests.sh;'''
			step([$class: 'MSTestPublisher', testResultsFile: '**/test/unit/**/*.trx', failOnError: true, keepLongStdio: true])
		}
		finally {
			sh '''sh build.ci.unittests.cleanup.sh;'''		
		}
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