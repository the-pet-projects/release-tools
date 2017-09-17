// vars/executeSshCommand.groovy

def call(String username, String password, String cmd) {
	def sshCmd = 'sshpass -p \'${password}\' ssh ${username}@10.0.1.5 \''${cmd}'\''
	sh sshCmd
}