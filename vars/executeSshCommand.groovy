// vars/executeSshCommand.groovy

def call(String cmd) {
	def sshCmd = 'ssh renato.renabee@10.0.1.5 \'' + cmd + '\''
	sh sshCmd
}