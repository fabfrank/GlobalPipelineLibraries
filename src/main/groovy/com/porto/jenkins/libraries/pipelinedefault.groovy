def configfront = carregaProp()

def call(Map pipelineParams) {

pipeline {
    agent {
        label "master"
    }
	
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
        gitLabConnection('GitLabCorp')
        timeout(time: 5, unit:'MINUTES')
        timestamps()
    }

    tools {
        jdk 'ibm-java-x86_64-71'
        maven 'apache-maven-2.2.1'
    }

    environment {
		pom=''
		CANAL_ROCKETCHAT="saconline"
		EMAIL_REPORT=''
		SONAR_TOKEN_USER="7e2f0c94bb0c4687dd8d90db536f170f3db355de"
		ENCODING="UTF-8"
		JAVA_SOURCE=1.7
		JAVA_TARGET=1.7
		MODULOEARNAME=''
		deploy=''
		SONAR_PROJECT_DESC="PR-2017-00049-SAC Online"
		SLEEPQG=2
		CHANGELOGS=''
    }

    stages {
		
		stage("SCM branch Master") {
			when {
				branch 'master'
			}
			
			steps {
				gitlabCommitStatus(name: 'SCM branch Master') {
					cleanWs()
					
					checkout scm
					
					git branch: pipelineParams.branch, credentialsId: 'GitCredentials', url: pipelineParams.scmUrl
				}
			}
			post {
				failure {
					script { env.FAILURE_STAGE = 'SCM branch Master' }
				}
				
				success {
				    createFileAppJson()
				}
			}
		}
		
		stage("SCM Auto Merge") {
			when {
				expression { env.BRANCH_NAME != 'master' }
			}

			steps {
				gitlabCommitStatus(name: 'SCM Auto Merge') {
					
					cleanWs()
			        
					checkout([
					  $class: 'GitSCM',
					  branches: scm.branches + [[name: "origin/${env.gitlabSourceBranch}"]],
					  extensions: scm.extensions + [[
						$class: 'CleanCheckout'],[
						$class: 'UserIdentity', 
							email: 'jenkinsCDS@portoseguro.com.br', name: 'JenkinsCDS'],[
						$class: 'PreBuildMerge',
						options: [
						  fastForwardMode: 'FF',
						  mergeRemote: 'origin',
						  mergeStrategy: 'default',
						  mergeTarget: 'master'
						]
					  ],[$class: 'PerBuildTag']],
					  userRemoteConfigs: scm.userRemoteConfigs
					])
				}
			}
			
			post {
				failure {
					script { env.FAILURE_STAGE = 'SCM Auto Merge' }
				}
				
				success {
				    createFileAppJson()
				}
			}
		}
		
		stage("Build Project") {
			when {
				expression { isChanged() }
			}
			
			steps {
				gitlabCommitStatus(name: 'Build Project') {
					script {
						sh 'mvn -Dmaven.test.failure.ignore clean -U install'
					}
				}
			}

			post {
				failure {
					script { env.FAILURE_STAGE = 'Build Project' }
				}

				success {
					script {
						sh '''git update-index --assume-unchanged Jenkinsfile
						      git update-index --assume-unchanged .bowerrc
						      git update-index --assume-unchanged .gitignore
						      git update-index --assume-unchanged .gitattributes'''
					}
				}

				always {
					script {
						pom = readMavenPom file: 'pom.xml'
						MODULOEARNAME = propValue('contexto')
						CHANGELOGS = getChangeString()
						EMAIL_REPORT = sh returnStdout: true, script: 'git log --pretty=\'%ae\' --since=\'3 minutes ago\''
					}
				}
			}
		}
		
		stage("Push Auto Merge") {
			when {
				expression { env.BRANCH_NAME != 'master' && isChanged() }
			}

			steps {
				gitlabCommitStatus(name: 'Push Auto Merge') {
					
					withEnv(["BRANCH_NAME=${BRANCH_NAME}"]) {
						
						sh '''v=`git remote -v`
							 origin=$(echo $v | grep -E "^([^\\s]*)" | cut -d \' \' -f1-1)
							 git push $origin HEAD:$BRANCH_NAME'''
					}
				}
			}

			post {
				failure {
					script { env.FAILURE_STAGE = 'Push Auto Merge' }
				}
			}
		}
		
		stage("Deploy artifacts Nexus/Jenkins") {
			when {
				expression { isChanged() }
			}

			steps {
				gitlabCommitStatus(name: 'Deploy Nexus') {
					//nexusArtifactUploader artifacts: [[artifactId: "${MODULOEARNAME}", classifier: '', file: "target/${MODULOEARNAME}-${pom.version}.zip", type: 'zip']], credentialsId: 'NEXUS_ADMIN', groupId: "${pom.groupId}", nexusUrl: 'li2745:8081', nexusVersion: 'nexus3', protocol: 'http', repository: 'maven-snapshot', version: "${pom.version}"
					archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.zip', fingerprint: true, onlyIfSuccessful: true
				}
			}

			post {
				failure {
					script { env.FAILURE_STAGE = 'Deploy artifacts Nexus/Jenkins' }
				}
			}
		}
		
		stage('SonarQube analysis') {
			when {
				expression { isChanged() }
			}
			
			steps {
				gitlabCommitStatus(name: 'SonarQube analysis') {
					build job: 'sonarWork_Pipeline', 
						parameters: [
							string(name: 'PROJECTBASEDIR', value: "${WORKSPACE}"), 
							string(name: 'CANAL_ROCKETCHAT', value: "${CANAL_ROCKETCHAT}"), 
							string(name: 'SONAR_BRANCH', value: "${BRANCH_NAME}"), 
							string(name: 'SONAR_PROJECT_DESC', value: "${SONAR_PROJECT_DESC}"), 
							string(name: 'SONAR_TOKEN_USER', value: "${SONAR_TOKEN_USER}"), 
							string(name: 'EMAIL_REPORT', value: "${EMAIL_REPORT}"), 
							string(name: 'ENCODING', value: "${ENCODING}"), 
							string(name: 'SLEEPQG', value: "${SLEEPQG}"), 
							string(name: 'CHANGELOGS', value: "${CHANGELOGS}"), 
							[$class: 'LabelParameterValue', name: 'node', label: "${NODE_NAME}"]], 
							wait: false
				}
			}
			
			post {
				failure {
					script { env.FAILURE_STAGE = 'SonarQube analysis' }
				}

				changed {
					notifySuccessful(env.CANAL_ROCKETCHAT, "${CHANGELOGS}" + '\n\n' + propValue('zelador') + ', aguardando sua aprovação para deploy no ambiente ' + propValue('deploy'), "${MODULOEARNAME}/${BRANCH_NAME}")
				}
			}
		}
		
		stage('Deploy App Ambiente Dev') {
			when {
				allOf {
					expression { isChanged() }
				}
			}
			
			agent {
				label propValue('deployDEV')
			}

			steps {
				gitlabCommitStatus(name: 'Deploy App') {
					dir("${PATHTMPDEPLOY}") {
						script {
							//sh "sh download_artifact_nexus.sh -a ${pom.groupId.replace('.','/')}:${MODULOEARNAME}:${pom.version} -e zip  -n ${REPO_NEXUS} -o ${MODULOEARNAME}.zip"
							sh "curl -sS ${BUILD_URL}artifact/target/${MODULOEARNAME}-${pom.version}.zip -o ${PATHAPPDEPLOY}/${MODULOEARNAME}.zip -v -R --location-trusted --fail"
							sh returnStdout: false, script: "./ihs_deploy ${PATHIHS} ${MODULOEARNAME} '' '' '' '' '' ${PATHAPPDEPLOY}"
						}
					}
				}
			}

			post {
				failure {
					script { env.FAILURE_STAGE = 'Deploy App' }
				}
				
				always {
					script {
						sh "rm -rf ${PATHAPPDEPLOY}/${MODULOEARNAME}.zip"
					}
				}
			}
		}
		
		stage("Executar Deploy?") {
			when {
				expression { isChanged() && env.BRANCH_NAME == 'master'}
			}

			steps {
			    script {
			    	env.EXECDEPLOY = input message: 'Executar o deploy?',
			        	parameters: [choice(name: 'Deploy de Aplicacao', choices: 'Não\nSim', description: 'Escolha "Sim" se você quer executar o deploy deste build no ambiente propValue("deploy")')]
			    }
			}
	    }
		
		stage('Deploy App') {
			when {
				allOf {
					expression { isChanged() }
					environment name: 'EXECDEPLOY', value: 'Sim'
				}
			}
			
			agent {
				label propValue('deployTST')
			}

			steps {
				gitlabCommitStatus(name: 'Deploy App') {
					dir("${PATHTMPDEPLOY}") {
						script {
							//sh "sh download_artifact_nexus.sh -a ${pom.groupId.replace('.','/')}:${MODULOEARNAME}:${pom.version} -e zip  -n ${REPO_NEXUS} -o ${MODULOEARNAME}.zip"
							sh "curl -sS ${BUILD_URL}artifact/target/${MODULOEARNAME}-${pom.version}.zip -o ${PATHAPPDEPLOY}/${MODULOEARNAME}.zip -v -R --location-trusted --fail"
							sh returnStdout: false, script: "./ihs_deploy ${PATHIHS} ${MODULOEARNAME} '' '' '' '' '' ${PATHAPPDEPLOY}"
						}
					}
				}
			}

			post {
				failure {
					script { env.FAILURE_STAGE = 'Deploy App' }
				}
				
				always {
					script {
						sh "rm -rf ${PATHAPPDEPLOY}/${MODULOEARNAME}.zip"
					}
				}
			}
		}
    }
	
	post {
		failure {
			notifyFailed(env.EMAIL_REPORT, env.CANAL_ROCKETCHAT, "${CHANGELOGS}", env.FAILURE_STAGE, "${MODULOEARNAME}/${BRANCH_NAME}")
		}
	}
}
}

def notifyStarted(CANAL_ROCKETCHAT, CHANGES, MODULOEARNAME) {
   rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: "${JENKINS_URL}static/ff676c77/images/headshot.png", channel: "${CANAL_ROCKETCHAT}", message: "STARTED: ${currentBuild.currentResult}", rawMessage: true 
}

def notifySuccessful(CANAL_ROCKETCHAT, CHANGES, MODULOEARNAME) {
    rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: "${JENKINS_URL}static/ff676c77/images/headshot.png", channel: "${CANAL_ROCKETCHAT}", message: "Build: ${currentBuild.currentResult}", rawMessage: true 
}
 
def notifyFailed(EMAIL_REPORT, CANAL_ROCKETCHAT, CHANGES, STAGEERROR, MODULOEARNAME) {
    rocketSend attachments: [[audioUrl: '', authorIcon: '', authorName: '', color: 'green', imageUrl: '', messageLink: '', text: "${CHANGES}", thumbUrl: '', title: "${MODULOEARNAME}", titleLink: "${env.BUILD_URL}", titleLinkDownload: '', videoUrl: '']], avatar: "${JENKINS_URL}static/ff676c77/images/headshot.png", channel: "${CANAL_ROCKETCHAT}", message: "Build: ${currentBuild.currentResult}", rawMessage: true
    
    emailext (
        to: "${EMAIL_REPORT}",
        subject: "${currentBuild.currentResult}: ${currentBuild.fullDisplayName}'",
        body: """Stage error: ${STAGEERROR} <br><br><br><br><p>Check console output at "<a href="${env.BUILD_URL}">${currentBuild.currentResult}]</a>"</p><br>CHANGES: ${CHANGES}""",
        recipientProviders: [[$class: 'DevelopersRecipientProvider']]
    )
}

def getModulesEAR() {
	sh returnStdout: true, script: '''EAR=
	for entry in *
	do
		if [ ! -f "$entry" ];then
			valor=`find ./"${entry}" -type d -name \'application\' | grep -o "\\(.*\\)/" | sort -u | head -5`
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

def createFileAppJson() {
	writeFile encoding: 'UTF-8', file: "${WORKSPACE}/.bowerrc", text: '''{
		"proxy" : " http://F0103972:fa09lusa@prxwg.portoseguro.brasil:3128",
		"https-proxy" : "http://F0103972:fa09lusa@prxwg.portoseguro.brasil:3128",
		"strict-ssl": false
	} 
'''
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