pipeline {
  agent any

  options {
    timestamps()
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
    skipDefaultCheckout(true)
  }

  parameters {
    string(name: 'IMAGE_TAG_OVERRIDE', defaultValue: '', description: 'Optional immutable image tag, for example v1.2.3')
    choice(name: 'DEPLOY_TARGET', choices: ['kind', 'cloud'], description: 'Deploy to local kind or the current Kubernetes context')
    booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Build and publish images without Kubernetes deployment')
  }

  environment {
    COMPOSE_PROJECT_NAME = 'teaching-platform-jenkins'
    KIND_CLUSTER_NAME = 'teaching-platform-jenkins'
    MICROSERVICES_DB_ROOT_PASSWORD = 'ci-root-password'
    INTERNAL_API_KEY = 'ci-internal-api-key'
    UNIFIED_PORT = '3000'
    JUDGE0_API_URL = 'http://127.0.0.1:9'
    JUDGE0_TIMEOUT_MS = '1000'
    JUDGE0_LOCAL_FALLBACK = 'true'
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
        sh '''
          set -e
          pwsh -File scripts/verify-public-api-coverage.ps1
          mvn -B test package
          (cd services/user-service && mvn -B test package)
          (cd services/learning-service && mvn -B test package)
          (cd services/assessment-service && mvn -B test package)
        '''
      }
    }

    stage('Integration Regression') {
      steps {
        sh '''
          set -e
          mkdir -p uploads ci-artifacts
          chmod -R a+rwX uploads
          docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml build
          docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml up -d
          for attempt in $(seq 1 60); do
            if curl --fail --silent http://localhost:3000/healthz | grep -qx ok; then break; fi
            if [ "$attempt" = 60 ]; then docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml ps; exit 1; fi
            sleep 2
          done
          pwsh -File scripts/microservices-smoke.ps1 -BaseUrl http://localhost:3000
          pwsh -File scripts/e2e-microservices.ps1 -UserUrl http://localhost:8082 -LearningUrl http://localhost:8083 -AssessmentUrl http://localhost:8084
          E2E_BASE_URL=http://localhost:3000 \
          E2E_USER_SERVICE_URL=http://localhost:8082 \
          E2E_LEARNING_SERVICE_URL=http://localhost:8083 \
          E2E_ASSESSMENT_SERVICE_URL=http://localhost:8084 \
          E2E_DB_HOST=127.0.0.1 \
          E2E_DB_PORT=3308 \
          E2E_DB_USERNAME=root \
          E2E_DB_PASSWORD="$MICROSERVICES_DB_ROOT_PASSWORD" \
          E2E_INTERNAL_API_KEY="$INTERNAL_API_KEY" \
          E2E_MATRIX_FILE=ci-artifacts/java-e2e-matrix.json \
          E2E_MATRIX_CSV_FILE=ci-artifacts/java-e2e-matrix.csv \
          mvn -B -Dtest=MicroserviceMainlineE2EScript -Drun.microservice.e2e=true test
          pwsh -File scripts/microservices-business-regression.ps1 -BaseUrl http://localhost:3000
        '''
      }
      post {
        always {
          sh '''
            mkdir -p ci-artifacts
            docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml ps > ci-artifacts/compose-ps.txt || true
            docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml logs --no-color > ci-artifacts/compose.log || true
            docker compose -f docker-compose.microservices.yml -f docker-compose.unified.yml down -v --remove-orphans || true
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
          sh '''
            set -e
            echo "$SWR_PASSWORD" | docker login "$SWR_REGISTRY" --username "$SWR_USERNAME" --password-stdin
            docker build -f docker/backend/Dockerfile -t "$SWR_REGISTRY/$SWR_ORG/teaching-platform-web-bff:$IMAGE_TAG" .
            docker build -f docker/gateway/Dockerfile -t "$SWR_REGISTRY/$SWR_ORG/teaching-platform-gateway:$IMAGE_TAG" docker/gateway
            docker build -f services/user-service/Dockerfile -t "$SWR_REGISTRY/$SWR_ORG/teaching-platform-user-service:$IMAGE_TAG" services/user-service
            docker build -f services/learning-service/Dockerfile -t "$SWR_REGISTRY/$SWR_ORG/teaching-platform-learning-service:$IMAGE_TAG" services/learning-service
            docker build -f services/assessment-service/Dockerfile -t "$SWR_REGISTRY/$SWR_ORG/teaching-platform-assessment-service:$IMAGE_TAG" services/assessment-service
            docker push "$SWR_REGISTRY/$SWR_ORG/teaching-platform-web-bff:$IMAGE_TAG"
            docker push "$SWR_REGISTRY/$SWR_ORG/teaching-platform-gateway:$IMAGE_TAG"
            docker push "$SWR_REGISTRY/$SWR_ORG/teaching-platform-user-service:$IMAGE_TAG"
            docker push "$SWR_REGISTRY/$SWR_ORG/teaching-platform-learning-service:$IMAGE_TAG"
            docker push "$SWR_REGISTRY/$SWR_ORG/teaching-platform-assessment-service:$IMAGE_TAG"
            mkdir -p publish-records
            printf 'commit=%s\\nbranch=%s\\nimage_tag=%s\\nregistry=%s\\n' "$GIT_COMMIT" "$BRANCH_NAME" "$IMAGE_TAG" "$SWR_REGISTRY/$SWR_ORG" > publish-records/metadata.txt
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
          sh '''
            set -e
            if [ "$DEPLOY_TARGET" = "kind" ]; then
              kind get clusters | grep -qx "$KIND_CLUSTER_NAME" || kind create cluster --name "$KIND_CLUSTER_NAME" --config k8s/kind-config.yaml --wait 120s
            fi
            DEPLOY_TARGET="$DEPLOY_TARGET" KIND_CLUSTER_NAME="$KIND_CLUSTER_NAME" bash scripts/k8s-deploy.sh
          '''
        }
      }
    }
  }

  post {
    always {
      sh '''
        mkdir -p ci-artifacts deploy-artifacts
        kubectl -n teaching-platform get all,pvc -o wide > deploy-artifacts/final-kubernetes-resources.txt 2>&1 || true
        kubectl -n teaching-platform get events --sort-by=.lastTimestamp > deploy-artifacts/final-events.txt 2>&1 || true
      '''
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      archiveArtifacts allowEmptyArchive: true, artifacts: 'ci-artifacts/**,publish-records/**,deploy-artifacts/**'
    }
  }
}
