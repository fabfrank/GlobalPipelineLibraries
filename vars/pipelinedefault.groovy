import java.text.SimpleDateFormat

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]
	
	def configfront = carregaProp()
	
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()
	
	pipeline {
		agent {
			label propValue("agentlabel")
		}

		/*triggers {
			cron('H 0 * * 1')
		}*/

		options {
			buildDiscarder(logRotator(numToKeepStr: '14'))
			gitLabConnection('GitLabCorp')
			timeout(time: 60, unit:'MINUTES')
			timestamps()
		}
		
		tools {
			jdk 'ibm-java-x86_64-71'
			maven 'apache-maven-2.2.1'
		}
		
		environment {
			pom=''
			ENCODING="UTF-8"
			JAVA_SOURCE=1.7
			JAVA_TARGET=1.7
			MODULOEARNAME=''
			SLEEPQG=2
			AMBDEPLOY = ''
			TIPOCOMPONENTE = ''

			CLUSTERDEPLOY=propValue("clusterdeploy")
			CREDENCRTC=propValue('credencialrtc')
			WORKITEM=propValue('workitem')
			COMPONENTE=propValue('nomeAplicacao')
			CANAL_ROCKETCHAT=propValue('canalrocket')
		}

		stages {
			stage("Checkout RTC") {

				steps {
					cleanWs()

					script {
						if (pipelineParams.EXCLUDECOMPONENTS.equals('NONE')) {
							checkout([$class: 'RTCScm', avoidUsingToolkit: false, buildTool: 'jazz', buildType: [buildStream: "${pipelineParams.STR}", clearLoadDirectory: true, 
								generateChangelogWithGoodBuild: true, loadDirectory: '', value: 'buildStream'], credentialsId: "${CREDENCRTC}", overrideGlobal: true, 
								serverURI: 'https://portoalm.portoseguro.com.br/ccm', timeout: 480])
						} else {
							checkout([$class: 'RTCScm', avoidUsingToolkit: false, buildTool: 'jazz', buildType: [buildStream: "${pipelineParams.STR}", clearLoadDirectory: true, 
								componentLoadConfig: 'excludeSomeComponents', componentsToExclude: "${pipelineParams.EXCLUDECOMPONENTS}", customizedSnapshotName: '', 
								generateChangelogWithGoodBuild: true, loadDirectory: '', loadPolicy: 'useComponentLoadConfig', processArea: '', value: 'buildStream'], 
								credentialsId: "${CREDENCRTC}", overrideGlobal: true, serverURI: 'https://portoalm.portoseguro.com.br/ccm', timeout: 480])
						}
					}
				}
				
				post {
					failure {
						script { env.FAILURE_STAGE = 'Checkout RTC' }
					}

					success {
						script {
							ARTHEFACT=getModuleParent().trim()
						}
					}
					always {
						script {
							pipelineParams.EMAIL_REPORT=pipelineParams.EMAIL_REPORT + ", " + propValue('email')
							CHANGELOGS = getChangeString()
						}
					}
				}
			}
			
			stage("Build Project") {
				when {
					expression { isChanged() || isTriggered()}
				}
		
				steps {
					dir("${ARTHEFACT}") {
						script {
							script {
								MODULOEARNAME=getModulesEAR().trim()
							}
							if (pipelineParams.SKIPTEST) {
								sh "mvn clean -DskipTests -U ${pipelineParams.BUILDPROFILE} install"
							} else {
								sh "mvn clean -Dmaven.test.failure.ignore -U ${pipelineParams.BUILDPROFILE} install"
							}
						}
					}
				}

				post {
					failure {
						script { env.FAILURE_STAGE = 'Build Project' }
					}

					success {
						dir("${ARTHEFACT}") {
							script {
								TIPOCOMPONENTE = (MODULOEARNAME.equals('NONE') ? 'jar' : 'ear')
								if (TIPOCOMPONENTE.equals('jar')) {
									MODULOEARNAME = "${ARTHEFACT}"
								}
								
								archiveArtifacts artifacts: "**/target/*.${TIPOCOMPONENTE}", fingerprint: true
							}
						}
					}
					changed {
						notifyEmail(pipelineParams.EMAIL_REPORT, env.CHANGELOGS)
					}
					always {
						dir("${ARTHEFACT}") {
							script {
								pom = readMavenPom file: 'pom.xml'
							}
						}
					}
				}
			}
			
			stage('SonarQube analysis') {
				when {
					expression { isChanged() || isTriggered() }
				}
				
				steps {
					build job: 'sonarWork_Pipeline', 
						parameters: [
							string(name: 'PROJECTBASEDIR', value: "${WORKSPACE}/${ARTHEFACT}"), 
							string(name: 'CANAL_ROCKETCHAT', value: "${CANAL_ROCKETCHAT}"), 
							string(name: 'SONAR_BRANCH', value: "${JOB_NAME}"), 
							string(name: 'SONAR_PROJECT_DESC', value: "${pipelineParams.SONAR_PROJECT_DESC}"), 
							string(name: 'EMAIL_REPORT', value: "${pipelineParams.EMAIL_REPORT}"), 
							string(name: 'ENCODING', value: "${ENCODING}"), 
							string(name: 'SLEEPQG', value: "${SLEEPQG}"), 
							string(name: 'CHANGELOGS', value: "${CHANGELOGS}"), 
							[$class: 'LabelParameterValue', name: 'node', label: "${NODE_NAME}"]], 
							wait: false
				}

				post {
					failure {
						script { env.FAILURE_STAGE = 'SonarQube analysis' }
					}
				}
			}
			
			stage("Executar Deploy em ambiente de Teste?") {
				when {
					expression { (isChanged() || isTriggered()) }
					environment name: 'pipelineParams.STAGEDEPLOY', value: 'true'
				}
				steps {
					
					notifyEmail(propValue("emailZelador"), "Executar Deploy em ambiente de Teste?")
					script {
						env.EXECDEPLOY = input message: 'Executar Deploy em ambiente de Teste?',
							parameters: [choice(name: 'Deploy de Aplicacao', choices: 'Não\nSim', description: 'Escolha "Sim" se você quer executar o deploy deste build no ambiente ' + propValue("deployTST"))], submitter: 'JazzUsers'
						
						if (EXECDEPLOY.equals('Não') ) {
							currentBuild.result = "ABORTED"
						}   
					}
				}
			}
			
			stage('Deploy App em ambiente de teste') {
				when {
					allOf {
						expression { isChanged() || isTriggered() }
						environment name: 'EXECDEPLOY', value: 'Sim'
					}
				}
				
				agent {
					label propValue('deployTST')
				}
				steps {
					script {

						AMBDEPLOY = propValue('deployTST')

						if (TIPOCOMPONENTE.equals('ear')) {
							sh "curl -sS ${BUILD_URL}artifact/${MODULOEARNAME}/target/${MODULOEARNAME}-${pom.version}.${TIPOCOMPONENTE} -o ${PATHAPPDEPLOY}/${MODULOEARNAME}.${TIPOCOMPONENTE} -v -R --location-trusted --fail"
							sh returnStdout: false, script: "${WRAPPER_WAS}/wrapper_was -lang jython -f ${WRAPPER_WAS}/installEar.py ${MODULOEARNAME} ${PATHAPPDEPLOY} ${MODULOEARNAME} ${AMBDEPLOY} ${CLUSTERDEPLOY} webserver1"
						} else {
							sh "curl -sS ${BUILD_URL}artifact/target/${MODULOEARNAME}-${pom.version}.${TIPOCOMPONENTE} -o ${pipelineParams.SHAREDLIBNAME}/${MODULOEARNAME}-${pom.version}.${TIPOCOMPONENTE} -v -R --location-trusted --fail"
						}
					}
				}
				post {
					failure {
						script { env.FAILURE_STAGE = 'Deploy App em ambiente de teste' }
					}
					
					always {
						script {
							sh "rm -rf ${PATHAPPDEPLOY}/${MODULOEARNAME}.${TIPOCOMPONENTE}"
						}
					}
				}
			}
		}
		post {
			success {
				script {
					if (!isChanged() && !isTriggered() ) {
						currentBuild.result = "ABORTED"
					}
				}
			}
			failure {
				notifyFailed(pipelineParams.EMAIL_REPORT, env.CANAL_ROCKETCHAT, "${CHANGELOGS}", env.FAILURE_STAGE, "${JOB_NAME}")
			}
		}
	}
}

def notifyStarted(CANAL_ROCKETCHAT, CHANGES, MODULOEARNAME) {
   rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: '${JENKINS_URL}static/ff676c77/images/headshot.png', channel: "${CANAL_ROCKETCHAT}", message: "STARTED: ${currentBuild.currentResult}", webhookToken: 'JyxPgTHx3EC6zNhab/d59qZ3AdGGNp47dihamry8wdzgZeohj4L9j5N6MTCNjKgoQb'
}

def notifySuccessful(CANAL_ROCKETCHAT, CHANGES, MODULOEARNAME) {
	rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: '${JENKINS_URL}static/ff676c77/images/headshot.png', channel: "${CANAL_ROCKETCHAT}", message: "Build: ${currentBuild.currentResult}", webhookToken: 'JyxPgTHx3EC6zNhab/d59qZ3AdGGNp47dihamry8wdzgZeohj4L9j5N6MTCNjKgoQb'
}
 
def notifyFailed(EMAIL_REPORT, CANAL_ROCKETCHAT, CHANGES, STAGEERROR, MODULOEARNAME) {
	rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: '${JENKINS_URL}static/ff676c77/images/headshot.png', channel: "${CANAL_ROCKETCHAT}", message: "Build: ${currentBuild.currentResult}", webhookToken: 'JyxPgTHx3EC6zNhab/d59qZ3AdGGNp47dihamry8wdzgZeohj4L9j5N6MTCNjKgoQb'
	
	notifyEmail(EMAIL_REPORT, CHANGES)
}

def notifyEmail(EMAIL_REPORT, CHANGES) {
	
	emailext (
		to: "${EMAIL_REPORT}",
		subject: "${currentBuild.currentResult}: ${currentBuild.fullDisplayName}'",
		body: """<p>Check console output at "<a href="${env.BUILD_URL}">${currentBuild.currentResult}]</a>"</p><br>CHANGES: ${CHANGES}""",
		recipientProviders: [[$class: 'DevelopersRecipientProvider']]
	)
}

def getModuleParent() {
	sh returnStdout: true, script: '''PARENT=
	for entry in *
	do
		if [ ! -f "$entry" ];then
			valor=`find ./"${entry}" -type f -name \'pom.xml\' | grep -o "\\(.*\\)/" | sort -u | head -5`
			if [ -n "$valor" ];then
				PARENT="$entry"
				break
			fi
		fi
	done
	echo $PARENT'''
}

def getModulesEAR() {
	sh returnStdout: true, script: '''EAR=NONE
	for entry in *
	do
		if [ ! -f "$entry" ];then
			valor=`find ./"${entry}" -type d -name \'application\' | grep -o "\\(.*\\)/" | sort -u | head -5`
			
			if [ ! -n "$valor" ];then
				valor=`find ./"${entry}" -type f -name \'application.xml\' | grep -o "\\(.*\\)/" | sort -u | head -5`
			fi
			
			if [ -n "$valor" ];then
				EAR="$entry"
				break
			fi
		fi
	done
	echo $EAR'''
}

@NonCPS
def getChangeString() {
	MAX_MSG_LEN = 100
	def changeString = ""
	echo "Gathering SCM changes"
	def changeLogSets = currentBuild.rawBuild.changeSets
	for (int i = 0; i < changeLogSets.size(); i++) {
		def entries = changeLogSets[i].items
		for (int j = 0; j < entries.length; j++) {
			def entry = entries[j]
			truncated_msg = entry.msg.take(MAX_MSG_LEN)
			changeString += " - ${truncated_msg} [${entry.author}]\n"
		}
	}
	if (!changeString) {
		changeString = " - No new changes"
	}
	return changeString
}
def isAPI(moduloear) {
	return moduloear.contains('API')
}

@NonCPS
def isTriggered() {
  return (currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause) != null)
}

def isChanged() {
	return !getChangeString().contains('No new changes')
}

def carregaProp() {
	node('master') {
		configfront = readJSON file: "${JENKINS_HOME}/configservers/${JOB_NAME}.json"
	}
}

def propValue(valor) {
	return configfront[valor]
}

def isSegundaFeira() {
	def retorno = false
	def date = new Date()
	sdf = new SimpleDateFormat("u")
	
	echo "Dia Semana: " + sdf.format(date)
	if (sdf.format(date).equals('1')) {
		retorno = true
	}
	return retorno
}