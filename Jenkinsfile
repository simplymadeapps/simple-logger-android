pipeline {
  agent { label "android" }

  environment {
  }

  stages {
    stage("File Setup") {
      steps {
        sh "mv .jenkins/ci-local.properties local.properties"
        sh "docker create -t -i -v $WORKSPACE:/opt/project-android --name jd-container simplymadeapps/docker-android:1.0.0"
        sh "docker start jd-container"
      }
    }
    
    // We are running as a circleci user and not as root so we have to add sudo to everything
    
    stage("Tests") {
     steps {   
        sh "docker exec jd-container sudo ./gradlew dependencies"        
        sh "docker exec jd-container sudo ./gradlew check"
     }
   	}
    
    stage("Coverage") {
      steps {
        sh "docker exec jd-container sudo ./gradlew jacocoTestReport"
        archiveArtifacts 'build/reports/jacoco/jacocoTestReport/html/**/*.*'
      }
    }
  }

  post {
    cleanup {
      sh 'docker rm -f $(docker ps -a -q)' // remove docker containers
      sh 'docker rmi -f $(docker images -a -q)' // remove docker images
      sh 'sudo rm -rf .gradle build'
      deleteDir()
    }
    
    failure {
      mail body: "<h2>Jenkins Build Failure</h2>Build Number: ${env.BUILD_NUMBER}<br>Branch: ${env.GIT_BRANCH}<br>Build URL: ${env.BUILD_URL}",
           charset: 'UTF-8',
           from: 'notice@simpleinout.com',
           mimeType: 'text/html',
           subject: "Jenkins Build Failure: ${env.JOB_NAME}",
           to: "stephen@simplymadeapps.com";
    }
  }
}