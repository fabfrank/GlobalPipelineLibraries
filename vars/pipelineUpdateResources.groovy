def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()
	
	pipeline {
		agent {
			label "master"
		}

		parameters {
			string(defaultValue: "", description: '', name: 'oldFileName')
			string(defaultValue: "", description: '', name: 'fileName')
			choice(choices: 'properties\nlog1\nlog2\nshared', description: 'Qual tipo de atualização?', name: 'tipoAtualizacao')
			string(defaultValue: "", description: '', name: 'fileBase64')
		}
	
		options {
			buildDiscarder(logRotator(numToKeepStr: '5'))
			gitLabConnection('GitLabCorp')
			timeout(time: 1, unit:'MINUTES')
			timestamps()
		}

		stages {
			stage("Criando Parametros Json") {

				steps {
					cleanWs()
					createFileAppJson(params.oldFileName, params.fileName, params.tipoAtualizacao, params.fileBase64)
				}
				
				post {
					failure {
						script { env.FAILURE_STAGE = 'Criando Parametros Json' }
					}
				}
			}
			
			stage("Executar Atualização") {

				steps {
					script {
						sh 'curl -vX POST http://li2390/calculoaceitacaoautoWS/rest/api/utilitarios-calculo-aceitacao/upload -d @dataFile.json --header "Content-Type: application/json"'
						sh 'curl -vX POST http://li2405:9089/calculoaceitacaoautoWS/rest/api/utilitarios-calculo-aceitacao/upload -d @dataFile.json --header "Content-Type: application/json"'
						sh 'curl -vX POST http://li2406:9089/calculoaceitacaoautoWS/rest/api/utilitarios-calculo-aceitacao/upload -d @dataFile.json --header "Content-Type: application/json"'
					}
				}
				
				post {
					failure {
						script { env.FAILURE_STAGE = 'Executar Atualização' }
					}
				}
			}
		}
		post {
			failure {
				notifyFailed("", "", "", env.FAILURE_STAGE, "${JOB_NAME}")
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

def createFileAppJson(oldFileName, fileName, tipoAtualizacao, fileBase64) {

	def pathConfig = ['properties':'/WebSphere/AppServer/lib/app/properties', 'log1':'/WebSphere/AppServer/lib/app/config/logs', 'log2':'/WebSphere/AppServer/lib/app/config/logs2', 'shared':'/WebSphere/was_shared_libraries/sharedLib_Auto']
	
	path = pathConfig["${tipoAtualizacao}"]
	
	String text = '''{
					"oldFileName": "oldFileNameValue",
					"fileName": "fileNameValue",
					"pathCopyFile": "pathValue",
					"dataFile": "fileBase64Value"
				}'''
				
				
	text = text.replace('oldFileNameValue',"${oldFileName}")
	text = text.replace('fileNameValue',"${fileName}")
	text = text.replace('pathValue',"${path}")
	text = text.replace('fileBase64Value',"${fileBase64}")
	
	writeFile encoding: 'UTF-8', file: 'dataFile.json', text: "${text}"
}
