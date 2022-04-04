#!/usr/bin/groovy

cancelPreviousBuilds()

appRoleName = 'trf-approle_jenkins-gitlab-branch-source-plugin'

def secrets = [[
    $class: 'VaultSecret', path: 'trf-secret/prod/aws-iam/laf-jenkins-gitlab-branch-source-plugin', secretValues: [
        [$class: 'VaultSecretValue', envVar: 'AWS_ACCESS_KEY_ID', vaultKey: 'access_key'],
        [$class: 'VaultSecretValue', envVar: 'AWS_SECRET_ACCESS_KEY', vaultKey: 'secret_key'],
    ]
]]

def configuration = [
    $class: 'VaultConfiguration',
    vaultUrl: VAULT_ADDR,
    vaultCredentialId: appRoleName
]

pipeline {

    options {
        ansiColor('xterm')
        timestamps()
    }

    agent none

    stages {
        stage('Build Plugin') {
            agent {
                kubernetes {
                    defaultContainer 'jnlp'
                    label "build_plugin_${UUID.randomUUID().toString().substring(0,5)}"
                    yaml """
                        apiVersion: v1
                        kind: Pod
                        spec:
                          containers:
                          - name: git
                            image: alpine/git
                            imagePullPolicy: Always
                            command:
                            - cat
                            tty: true
                            resources:
                              requests:
                                cpu: 1
                                memory: 1Gi
                              limits:
                                cpu: 1
                                memory: 1Gi
                          - name: maven
                            image: maven
                            imagePullPolicy: Always
                            command:
                            - cat
                            tty: true
                            resources:
                              requests:
                                cpu: 2
                                memory: 4Gi
                              limits:
                                cpu: 2
                                memory: 4Gi
                          - name: aws-utils
                            image: 226567326923.dkr.ecr.eu-west-3.amazonaws.com/core/application/aws-utils:0.0.20
                            imagePullPolicy: Always
                            command:
                            - /bin/bash
                            - -l
                            - -c
                            - cat
                            tty: true
                            resources:
                              requests:
                                cpu: 1
                                memory: 1024Mi
                              limits:
                                cpu: 1
                                memory: 1024Mi
                    """
                }
            }
            when {
                beforeAgent true
                buildingTag()
            }
            steps {
                container('git') {
                    script {
                        sh """
                        git clone https://github.com/lafourchette/gitlab-branch-source-plugin.git
                        """
                    }
                }
                container('maven') {
                    script {
                        sh """
                        cd gitlab-branch-source-plugin
                        apt update && apt install -y python-pip && pip install awscli
                        mvn versions::set -DnewVersion=${env.BRANCH_NAME}
                        mvn clean install -DskipTests=true -Dmaven.javadoc.skip=true -B -V -Daccess-modifier-checker.failOnError=true -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
                        """
                    }
                }
                container('aws-utils') {
                    wrap([$class: 'VaultBuildWrapper', configuration: configuration, vaultSecrets: secrets]) {
                        script {
                            sh """
                            cd gitlab-branch-source-plugin
                            aws s3 cp target/gitlab-branch-source.hpi s3://trf-jenkins-gitlab-branch-source-plugin/gitlab-branche-source-${env.BRANCH_NAME}.hpi
                            """
                        }
                    }
                }
            }
        }
    }
}

