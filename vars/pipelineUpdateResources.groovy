def call(body) {
    // evaluate the body block, and collect configuration into the object
    def pipelineParams= [:]

	body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()
	
	pipeline {
    	agent {
    		label 'master'
    	}
    
    	triggers {
    		cron('H 2 * * 1-5')
    	}
    
    	options {
    		buildDiscarder(logRotator(numToKeepStr: '15'))
    		gitLabConnection('GitLabCorp')
    		timeout(time: 120, unit:'MINUTES')
    		timestamps()
    	}
    	
    	tools {
    		jdk 'jdk1.8.0_121'
    	}
    	
    	environment {
    		ENCODING='UTF-8'
    		JAVA_SOURCE=1.7
    		JAVA_TARGET=1.7
    	}
    
    	stages {
            stage('Preparando Rercursos') {
                steps {
    				cleanWs()
    			}
    			
    			post {
    				failure {
    					script { env.FAILURE_STAGE = 'Preparando Rercursos' }
    				}
    			}
    		}
            		
    		stage("'Run Tests JMeter") {
                parallel {
                    stage('Centralização de Calculo') {
                        when {
        					environment name: 'STAGECALCULO', value: 'true'
        				}
            			steps {
            				cleanWs()
            
            				script {
            					sh returnStatus: true, script: '/desenv/apache-jmeter-4.0/bin/jmeter.sh -Jjmeter.save.saveservice.output_format=xml -n -t /desenv/apache-jmeter-4.0/centralizacao/TesteCentralizacao_SOAP_REST_calculo_jenkins.jmx -l TesteCentralizacao_SOAP_REST_calculo_jenkins.jtl'
            				}
            			}
            			
            			post {
            				failure {
            					script { env.FAILURE_STAGE = 'Centralização de Calculo' }
            				}
            			}
            		}
        		    stage('Centralização de Consulta') {
        		        when {
        					environment name: 'STAGECONSULTA', value: 'true'
        				}

            			steps {
            				script {
            					sh returnStatus: true, script: '/desenv/apache-jmeter-4.0/bin/jmeter.sh -Jjmeter.save.saveservice.output_format=xml -n -t /desenv/apache-jmeter-4.0/consulta/TesteCentralizacao_SOAP_REST_jenkins.jmx -l TesteCentralizacao_SOAP_REST_jenkins.jtl'
            				}
            			}
            			
            			post {
            				failure {
            					script { env.FAILURE_STAGE = 'Centralização de Consulta' }
            				}
            			}
            		}
            	}
    	    }
        }
        post {
    		failure {
    			notifyFailed('', env.CANAL_ROCKETCHAT, '', env.FAILURE_STAGE, "${JOB_NAME}")
    		}
    		success {
    		    perfReport errorFailedThreshold: 0, errorUnstableThreshold: 0, modePerformancePerTestCase: true, modeThroughput: true, percentiles: '0,50,90,100', sourceDataFiles: '**/*.jtl'
    		}
    		always {
    			logstashSend failBuild: false, maxLines: 1000
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

@NonCPS
def isTriggered() {
  return (currentBuild.rawBuild.getCause(hudson.model.Cause$UpstreamCause) != null)
}

def isChanged() {
	return !getChangeString().contains('No new changes')
}
