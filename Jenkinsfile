pipeline {
  agent { label "android" }

  environment {
  	CODECOV_TOKEN = credentials("AMAZON_LOGGER_ANDROID_CODECOV_TOKEN")
  }

  stages {
    stage("Build") {
      steps {
      	sh "sudo chmod +x ./gradlew"
        sh "mv .jenkins/ci-local.properties local.properties"
        sh "mv .jenkins/ci-gradle.properties gradle.properties"
        sh "docker create -t -i -v $WORKSPACE:/opt/project-android --name jd-container simplymadeapps/docker-android:1.0.0"
        sh "docker start jd-container"
        sh "docker ps -a"
        sh "docker exec jd-container ls -lsa"
      }
    }

   stage("Tests") {
     steps {   
        sh "docker exec jd-container sudo ./gradlew dependencies"        
        sh "docker exec jd-container sudo ./gradlew check"
        sh "docker exec jd-container sudo ./gradlew jacocoTestReport"
		sh "docker exec jd-container sudo bash -c 'bash <(curl -s https://codecov.io/bash) -t $CODECOV_TOKEN'"
     }
   	}
  }

  post {
    cleanup {
      /// do a cleanup within the Docker container which operates as root
      sh "docker stop jd-container || true"
      sh "docker rm jd-container || true"
      sh 'sudo rm -rf .gradle build'
      deleteDir()
      sh "ls -lsa"
    }
  }
}