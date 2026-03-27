pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }

  parameters {
    booleanParam(name: 'DEPLOY', defaultValue: false, description: '构建完成后是否自动部署到 ECS')
  }

  environment {
    // ===== 需要按你的环境调整 =====
    DEPLOY_HOST = '121.41.27.176'
    DEPLOY_USER = 'root'
    SSH_CREDENTIALS_ID = 'ecs-ssh'

    // 后端
    BACKEND_JAR_LOCAL = 'target/springboot-mybatis-quickstart-0.0.1-SNAPSHOT.jar'
    BACKEND_JAR_REMOTE = '/opt/tennis-court-booking/runtime/app.jar'
    BACKEND_SERVICE = 'tennis-court-booking'

    // 前端
    FRONTEND_DIR = 'tennis-court-frontend'
    FRONTEND_DIST_LOCAL = 'tennis-court-frontend/dist/'
    FRONTEND_DIST_REMOTE = '/var/www/tennis/dist/'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build Backend') {
      steps {
        sh './mvnw -DskipTests clean package'
      }
    }

    stage('Build Frontend') {
      steps {
        dir("${env.FRONTEND_DIR}") {
          sh 'npm ci || npm install'
          sh 'npm run build'
        }
      }
    }

    stage('Deploy Backend') {
      when { expression { return params.DEPLOY } }
      steps {
        sshagent(credentials: ["${env.SSH_CREDENTIALS_ID}"]) {
          sh '''
            set -e
            scp -o StrictHostKeyChecking=no "${BACKEND_JAR_LOCAL}" "${DEPLOY_USER}@${DEPLOY_HOST}:${BACKEND_JAR_REMOTE}"
            ssh -o StrictHostKeyChecking=no "${DEPLOY_USER}@${DEPLOY_HOST}" "systemctl restart ${BACKEND_SERVICE} && systemctl is-active ${BACKEND_SERVICE}"
          '''
        }
      }
    }

    stage('Deploy Frontend') {
      when { expression { return params.DEPLOY } }
      steps {
        sshagent(credentials: ["${env.SSH_CREDENTIALS_ID}"]) {
          sh '''
            set -e
            rsync -az --delete -e "ssh -o StrictHostKeyChecking=no" "${FRONTEND_DIST_LOCAL}" "${DEPLOY_USER}@${DEPLOY_HOST}:${FRONTEND_DIST_REMOTE}"
            ssh -o StrictHostKeyChecking=no "${DEPLOY_USER}@${DEPLOY_HOST}" "nginx -t && systemctl reload nginx"
          '''
        }
      }
    }

    stage('Health Check') {
      when { expression { return params.DEPLOY } }
      steps {
        sshagent(credentials: ["${env.SSH_CREDENTIALS_ID}"]) {
          sh '''
            set -e
            ssh -o StrictHostKeyChecking=no "${DEPLOY_USER}@${DEPLOY_HOST}" "curl -s -o /dev/null -w '%{http_code}\\n' http://127.0.0.1:8080/api/login"
          '''
        }
      }
    }
  }

  post {
    success {
      echo 'CI/CD completed successfully.'
    }
    failure {
      echo 'CI/CD failed. Please inspect stage logs.'
    }
  }
}
