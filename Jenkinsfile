// Reference: https://github.com/jenkinsci/pipeline-model-definition-plugin/wiki/Syntax-Reference

pipeline {
  agent {
    docker {
      image 'java:8'
      args """-u root -v \$HOME/.gradle/caches/${env.JOB_NAME}:/root/.gradle/caches:rw
                -v \$HOME/.gradle/wrapper:/root/.gradle/wrapper:rw
            """
      }
  }
  environment {
      ARTIFACTORY_PASS = credentials('Artifactory')
      YUM_KEY = credentials('aam-yum-secret-file')
      YUM_PASSPHRASE = credentials('aam-yum-ssh-passphrase')

  }

  stages {
      stage('Setup env') {
        steps {
          setupGradleEnv()
        }
      }

      stage('Build & Test') {
          steps {
              sh './gradlew clean build --info'
              junit allowEmptyResults: true, testResults: "build/test-results/**/TEST-*.xml"
          }
      }

      //
      stage('Publish Yum') {
          steps {
              sh './gradlew publishFast --info'
          }
      }
      //

      //

  }

  post {
      always {
        archive '**/*.jar'
      }
  }
}
