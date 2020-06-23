pipeline {

    agent {
        label "master"
    }

    environment {
        // GLobal Vars
        PIPELINES_NAMESPACE = "labs-ci-cd"
        NAME = "sc-routes"

        // Job name contains the branch eg my-app-feature%2Fjenkins-123
        JOB_NAME = "${JOB_NAME}".replace("%2F", "-").replace("/", "-")
        IMAGE_REPOSITORY= 'image-registry.openshift-image-registry.svc:5000'

        GIT_REPO = "https://github.com/eformat/sc-routes.git#${GIT_BRANCH}"
        S2I_IMAGE = 'quay.io/quarkus/ubi-quarkus-native-s2i:20.1.0-java11'
        GIT_SSL_NO_VERIFY = true

        // Credentials bound in OpenShift
        GIT_CREDS = credentials("${PIPELINES_NAMESPACE}-git-auth")
        NEXUS_CREDS = credentials("${PIPELINES_NAMESPACE}-nexus-password")
        ARGOCD_CREDS = credentials("${PIPELINES_NAMESPACE}-argocd-token")

        // Nexus Artifact repo
        NEXUS_REPO_NAME="labs-static"
        NEXUS_REPO_HELM = "helm-charts"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '50', artifactNumToKeepStr: '1'))
        timeout(time: 15, unit: 'MINUTES')
    }

    stages {
        stage('Perpare Environment') {
            failFast true
            parallel {
                stage("Release Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("master") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            env.STAGING_NAMESPACE = "labs-staging"
                            env.APP_NAME = "${NAME}".replace("/", "-").toLowerCase()
                        }
                    }
                }
                stage("Sandbox Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("dev") || GIT_BRANCH.startsWith("feature") || GIT_BRANCH.startsWith("fix") || GIT_BRANCH.startsWith("nsfw") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            // ammend the name to create 'sandbox' deploys based on current branch
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
                    }
                }
                stage("Pull Request Build") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    when {
                        expression { GIT_BRANCH.startsWith("PR-") }
                    }
                    steps {
                        script {
                            env.TARGET_NAMESPACE = "labs-dev"
                            env.APP_NAME = "${GIT_BRANCH}-${NAME}".replace("/", "-").toLowerCase()
                        }
                    }
                }
            }
        }

        stage("Build and Deploy") {
            parallel {
                stage("Build App") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    steps {
                        script {
                            sh '''
                            oc -n ${TARGET_NAMESPACE} get bc ${NAME} || rc=$?
                            if [ $rc -eq 1 ]; then
                                echo " 🏗 no app build - creating one, make sure secret ${NAME} exists first 🏗"
                                oc -n ${TARGET_NAMESPACE} new-app --as-deployment-config ${S2I_IMAGE}~${GIT_REPO} --name=${NAME}
                                oc -n ${TARGET_NAMESPACE} patch bc/sc-routes -p '{"spec":{ "runPolicy": "Parallel"}}' --type=strategic
                                oc -n ${TARGET_NAMESPACE} env --from=secret/sc-routes dc/sc-routes
                                oc -n ${TARGET_NAMESPACE} logs -f bc/${NAME}
                            fi
                            echo " 🏗 build found - starting it  🏗"
                            oc -n ${TARGET_NAMESPACE} start-build ${NAME} --follow
                            oc -n ${TARGET_NAMESPACE} expose svc/${NAME}
                            '''
                        }
                    }
                }
                stage("Infinispan") {
                    agent {
                        node {
                            label "master"
                        }
                    }
                    steps {
                        script {
                            sh '''
                            oc -n ${TARGET_NAMESPACE} get infinispan infinispan || rc=$?
                            if [ $rc -eq 1 ]; then
                                echo " 🏗 no infinispan cluster - creating 🏗"
                                oc -n ${TARGET_NAMESPACE} apply -f ocp/infinispan-subscription.yaml
                                oc -n ${TARGET_NAMESPACE} apply -f ocp/infinispan-operatorgroup.yaml
                                sleep 10
                                oc -n ${TARGET_NAMESPACE} wait pod -l name=infinispan-operator-alm-owned --for=condition=Ready --timeout=300s
                                oc -n ${TARGET_NAMESPACE} apply -f ocp/infinispan-cr.yaml
                                sleep 10
                                oc -n ${TARGET_NAMESPACE} wait pod -l app=infinispan-pod --for=condition=Ready --timeout=300s
                            fi
                            echo " 🏗 found infinispan cluster - skipping  🏗"                            
                            '''
                        }
                    }
                }
            }
        }
    }
}
