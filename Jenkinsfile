pipeline {
  agent { label 'local-docker-desktop' }

  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    skipDefaultCheckout(true)
    disableConcurrentBuilds()
  }

  // Local Jenkins is not publicly reachable, so poll GitHub once per minute.
  triggers {
    pollSCM('* * * * *')
  }

  parameters {
    string(name: 'IMAGE_TAG_OVERRIDE', defaultValue: '', description: 'Optional immutable image tag, for example v1.2.3')
    choice(name: 'DEPLOY_TARGET', choices: ['local', 'cloud'], description: 'Deploy to this Jenkins host Docker Desktop Kubernetes or the configured cloud context')
    booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Build and publish images without Kubernetes deployment')
  }

  environment {
    COMPOSE_PROJECT_NAME = 'teaching-platform-jenkins'
    MICROSERVICES_DB_ROOT_PASSWORD = 'ci-root-password'
    INTERNAL_API_KEY = 'ci-internal-api-key'
    UNIFIED_PORT = '3000'
    JUDGE0_API_URL = 'http://127.0.0.1:9'
    JUDGE0_TIMEOUT_MS = '1000'
    JUDGE0_LOCAL_FALLBACK = 'true'
    KUBECONFIG = 'C:\\Users\\Lenovo\\.kube\\config'
    JAVA_HOME = 'C:\\Program Files\\Java\\jdk1.8.0_351'
    PYTHON_EXE = 'D:\\Programs\\Python\\Python312\\python.exe'
    MAVEN_IMAGE = 'docker.m.daocloud.io/library/maven:3.8.8-eclipse-temurin-8'
    JAVA_IMAGE = 'docker.m.daocloud.io/library/eclipse-temurin:8-jdk-jammy'
    NGINX_IMAGE = 'docker.m.daocloud.io/library/nginx:1.27-alpine'
    MICROSERVICES_MYSQL_IMAGE = 'docker.m.daocloud.io/library/mysql:8.0.43'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.IMAGE_TAG = params.IMAGE_TAG_OVERRIDE?.trim()
          if (!env.IMAGE_TAG) {
            env.IMAGE_TAG = "sha-${env.GIT_COMMIT.substring(0, 12)}"
          }
        }
      }
    }

    stage('Build And Test') {
      steps {
        bat '''
          @echo off
          powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\verify-public-api-coverage.ps1 || exit /b 1
          call "C:\\getMvn\\apache-maven-3.5.3\\bin\\mvn.cmd" -B test package || exit /b 1
          pushd services\\user-service
          call "C:\\getMvn\\apache-maven-3.5.3\\bin\\mvn.cmd" -B test package || exit /b 1
          popd
          pushd services\\learning-service
          call "C:\\getMvn\\apache-maven-3.5.3\\bin\\mvn.cmd" -B test package || exit /b 1
          popd
          pushd services\\assessment-service
          call "C:\\getMvn\\apache-maven-3.5.3\\bin\\mvn.cmd" -B test package || exit /b 1
          popd
        '''
      }
    }

    stage('Integration Regression') {
      steps {
        bat '''
          @echo off
          if not exist uploads mkdir uploads
          if not exist ci-artifacts mkdir ci-artifacts
          docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml build || exit /b 1
          docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up -d || exit /b 1
          powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ready=$false; 1..60 | ForEach-Object { try { if ((Invoke-WebRequest 'http://localhost:3000/healthz' -UseBasicParsing -TimeoutSec 3).Content.Trim() -eq 'ok') { $ready=$true; return } } catch {}; Start-Sleep -Seconds 2 }; if (-not $ready) { exit 1 }" || exit /b 1
          powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\microservices-smoke.ps1 -BaseUrl http://localhost:3000 || exit /b 1
          powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\e2e-microservices.ps1 -UserUrl http://localhost:8082 -LearningUrl http://localhost:8083 -AssessmentUrl http://localhost:8084 || exit /b 1
          powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\microservices-business-regression.ps1 -BaseUrl http://localhost:3000 || exit /b 1
        '''
      }
      post {
        always {
          bat '''
            @echo off
            if not exist ci-artifacts mkdir ci-artifacts
            docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml ps > ci-artifacts\\compose-ps.txt 2>&1
            docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml logs --no-color > ci-artifacts\\compose.log 2>&1
            docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml down -v --remove-orphans
            exit /b 0
          '''
        }
      }
    }

    stage('Publish Versioned Images') {
      steps {
        withCredentials([
          string(credentialsId: 'swr-registry', variable: 'SWR_REGISTRY'),
          string(credentialsId: 'swr-org', variable: 'SWR_ORG'),
          usernamePassword(credentialsId: 'swr-account', usernameVariable: 'SWR_USERNAME', passwordVariable: 'SWR_PASSWORD')
        ]) {
          bat '''
            @echo off
            echo %SWR_PASSWORD%| docker login %SWR_REGISTRY% --username %SWR_USERNAME% --password-stdin || exit /b 1
            docker build --provenance=false --build-arg MAVEN_IMAGE=%MAVEN_IMAGE% --build-arg JAVA_IMAGE=%JAVA_IMAGE% -f docker/backend/Dockerfile -t %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-web-bff:%IMAGE_TAG% . || exit /b 1
            docker build --provenance=false --build-arg NGINX_IMAGE=%NGINX_IMAGE% -f docker/gateway/Dockerfile -t %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-gateway:%IMAGE_TAG% docker/gateway || exit /b 1
            docker build --provenance=false --build-arg MAVEN_IMAGE=%MAVEN_IMAGE% --build-arg JAVA_IMAGE=%JAVA_IMAGE% -f services/user-service/Dockerfile -t %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-user-service:%IMAGE_TAG% services/user-service || exit /b 1
            docker build --provenance=false --build-arg MAVEN_IMAGE=%MAVEN_IMAGE% --build-arg JAVA_IMAGE=%JAVA_IMAGE% -f services/learning-service/Dockerfile -t %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-learning-service:%IMAGE_TAG% services/learning-service || exit /b 1
            docker build --provenance=false --build-arg MAVEN_IMAGE=%MAVEN_IMAGE% --build-arg JAVA_IMAGE=%JAVA_IMAGE% -f services/assessment-service/Dockerfile -t %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-assessment-service:%IMAGE_TAG% services/assessment-service || exit /b 1
            docker build --provenance=false --build-arg MYSQL_IMAGE=%MICROSERVICES_MYSQL_IMAGE% -f docker/mysql-init/Dockerfile -t %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-microservices-mysql:%IMAGE_TAG% . || exit /b 1
            docker push %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-web-bff:%IMAGE_TAG% || exit /b 1
            docker push %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-gateway:%IMAGE_TAG% || exit /b 1
            docker push %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-user-service:%IMAGE_TAG% || exit /b 1
            docker push %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-learning-service:%IMAGE_TAG% || exit /b 1
            docker push %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-assessment-service:%IMAGE_TAG% || exit /b 1
            docker push %SWR_REGISTRY%/%SWR_ORG%/teaching-platform-microservices-mysql:%IMAGE_TAG% || exit /b 1
            if not exist publish-records mkdir publish-records
            > publish-records\\metadata.txt echo commit=%GIT_COMMIT%
            >> publish-records\\metadata.txt echo branch=%BRANCH_NAME%
            >> publish-records\\metadata.txt echo image_tag=%IMAGE_TAG%
          '''
        }
      }
    }

    stage('Deploy Kubernetes') {
      when {
        expression { return !params.SKIP_DEPLOY }
      }
      steps {
        withCredentials([
          string(credentialsId: 'swr-registry', variable: 'SWR_REGISTRY'),
          string(credentialsId: 'swr-org', variable: 'SWR_ORG'),
          usernamePassword(credentialsId: 'swr-account', usernameVariable: 'SWR_USERNAME', passwordVariable: 'SWR_PASSWORD'),
          string(credentialsId: 'db-root-password', variable: 'DB_ROOT_PASSWORD')
        ]) {
          withEnv(["DEPLOY_TARGET=${params.DEPLOY_TARGET}"]) {
            bat '"C:\\Program Files\\Git\\bin\\bash.exe" scripts/k8s-deploy.sh'
          }
        }
      }
    }
  }

  post {
    always {
      bat '''
        @echo off
        if not exist ci-artifacts mkdir ci-artifacts
        if not exist deploy-artifacts mkdir deploy-artifacts
        kubectl -n teaching-platform get all,pvc -o wide > deploy-artifacts\\final-kubernetes-resources.txt 2>&1
        kubectl -n teaching-platform get events --sort-by=.lastTimestamp > deploy-artifacts\\final-events.txt 2>&1
        exit /b 0
      '''
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      archiveArtifacts allowEmptyArchive: true, artifacts: 'ci-artifacts/**,publish-records/**,deploy-artifacts/**'
    }
  }
}
