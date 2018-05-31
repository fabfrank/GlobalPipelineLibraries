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
    		choice(choices: 'COTACAOAUTO_TST\nCOTACAOAUTO_HML\nCOTACAOAUTO\nWAS8TARIFA_HML\nLI2233', description: 'Informar o ambiente para limpar o cache serviços de consulta!', name: 'AMBIENTE')
    	}
    
    	/*triggers {
            cron('H 0 * * 1')
        }*/
    
    	options {
    		buildDiscarder(logRotator(numToKeepStr: '3'))
    		gitLabConnection('GitLabCorp')
    		timeout(time: 60, unit:'MINUTES')
    		timestamps()
    	}
    	
    	/*environment {
    		CLUSTERDEPLOY=propValue("clusterdeploy")
    		CREDENCRTC=propValue('credencialrtc')
    		WORKITEM=propValue('workitem')
    		COMPONENTE=propValue('nomeAplicacao')
    		CANAL_ROCKETCHAT=propValue('canalrocket')
    	}*/
    
    	stages {
    		
    		stage("Limpando Caches servicos-consultas") {
    		
    			steps {
    				script {
    				    switch (params.AMBIENTE) {
					    case 'COTACAOAUTO_TST':
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2390/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2390/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2390/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2390/historicoautoWS/rest/api/cache-service/clear'
						break
					    case 'COTACAOAUTO_HML':
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2813:9082/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2814:9082/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2813:9082/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2814:9082/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2813:9084/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2814:9084/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2813:9085/historicoautoWS/rest/api//cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2814:9085/historicoautoWS/rest/api//cache-service/clear' 
						break
					    case 'COTACAOAUTO':
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9091/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9092/historicoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9092/historicoautoWS/rest/api/cache-service/clear'

						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9085/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9086/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'

						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9091/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9092/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9092/controladorrolloutautoWS/rest/api/cache-service/clear'

						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9085/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3145:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3146:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3147:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3148:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3149:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3150:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3155:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3156:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3157:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3158:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3159:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li3160:9086/parametrizacaoautoWS/rest/api/cache-service/clear'
						break
					    case 'WAS8TARIFA_HML':
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2405:9087/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2406:9087/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2405:9087/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2406:9087/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2405:9091/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2406:9091/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2405:9090/historicoautoWS/rest/api//cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2406:9090/historicoautoWS/rest/api//cache-service/clear' 
						break
					     case 'LI2233':
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2233/centralizadoraceitacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2233/controladorrolloutautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2233/parametrizacaoautoWS/rest/api/cache-service/clear'
						sh 'curl -i -H "Accept: application/json" -H "Content-Type: application/json" -X GET http://li2233/historicoautoWS/rest/api/cache-service/clear'
						break
					}
    				}
    			}
    
    			post {
    				failure {
    					script { env.FAILURE_STAGE = 'Limpando Caches servicos-consultas' }
    				}
    
    				/*success {
    					script {
    					}
    				}*/
    			}
    		}
    	}
    	/*post {
    		failure {
    			notifyFailed(env.EMAIL_REPORT, env.CANAL_ROCKETCHAT, "${CHANGELOGS}", env.FAILURE_STAGE, "${JOB_NAME}")
    		}
    	}*/
    }
}
def notifyStarted(CANAL_ROCKETCHAT, CHANGES, MODULOEARNAME) {
   rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: '${JENKINS_URL}static/ff676c77/images/headshot.png', channel: "${CANAL_ROCKETCHAT}", message: "STARTED: ${currentBuild.currentResult}", webhookToken: 'ARdysxaddCFn6gQ5D/7JLMyqWKuptgJpH4zj7BYpHwagepz9RbJ5xjMG2gApBJa4mR'
}

def notifySuccessful(CANAL_ROCKETCHAT, CHANGES, MODULOEARNAME) {
    rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: '${JENKINS_URL}static/ff676c77/images/headshot.png', channel: "${CANAL_ROCKETCHAT}", message: "Build: ${currentBuild.currentResult}", webhookToken: 'ARdysxaddCFn6gQ5D/7JLMyqWKuptgJpH4zj7BYpHwagepz9RbJ5xjMG2gApBJa4mR'
}
 
def notifyFailed(EMAIL_REPORT, CANAL_ROCKETCHAT, CHANGES, STAGEERROR, MODULOEARNAME) {
    rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: '${JENKINS_URL}static/ff676c77/images/headshot.png', channel: "${CANAL_ROCKETCHAT}", message: "Build: ${currentBuild.currentResult}", webhookToken: 'ARdysxaddCFn6gQ5D/7JLMyqWKuptgJpH4zj7BYpHwagepz9RbJ5xjMG2gApBJa4mR'
    
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
