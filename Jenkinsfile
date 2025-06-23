pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '340752824368'
        ECR_REPO = 'flaskapp'
        // KUBECONFIG should be set via withCredentials block, not here.
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }

        stage('Clone Repository') {
            steps {
                checkout([$class: 'GitSCM',
                    branches: [[name: '*/main']],
                    userRemoteConfigs: [[
                        url: 'https://github.com/saiprathap-projects/daily_weather.git',
                        credentialsId: 'git-cred'
                    ]]
                ])
            }
        }

        stage('Login to ECR') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'aws-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                        aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
                        aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
                        aws configure set region $AWS_REGION
                        aws ecr get-login-password --region $AWS_REGION | \
                        docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
                    '''
                }
            }
        }

        stage('Terraform - Create ECR') {
            steps {
                script {
                    def mavenRepo = sh(script: "aws ecr describe-repositories --repository-names maven-build --region ${env.AWS_REGION}", returnStatus: true)
                    def tomcatRepo = sh(script: "aws ecr describe-repositories --repository-names tomcat --region ${env.AWS_REGION}", returnStatus: true)

                    if (mavenRepo != 0 || tomcatRepo != 0) {
                        withCredentials([usernamePassword(credentialsId: 'aws-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                            sh '''
                                cd terraform/ECR
                                terraform init
                                terraform apply -auto-approve
                            '''
                        }
                    } else {
                        echo "ECR repositories already exist. Skipping Terraform apply."
                    }
                }
            }
        }

        stage('maven build') {
            steps {
                sh '''
                    docker run --rm -v $PWD:/app -w /app maven:3.9.6 mvn clean package -DskipTests
                    cp target/*.war tomcat/ 
                '''
            }
        }
        stage( 'Build Tomcat Image') {
            steps {
                sh '''
                    docker build -t $ECR_REPO:latest ./tomcat
                '''
            }
        }
        stage('Tag & Push image to ECR') {
            steps {
                script {
                    def ecrUrl = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
                    def commitId = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    def versionTag = "v${env.BUILD_NUMBER}-${commitId}"
                    def images = ['tomcat']

                    images.each { imageName ->
                        def localImage = "${imageName}:latest"
                        def latestTag = "${ecrUrl}/${imageName}:latest"
                        def versionedTag  = "${ecrUrl}/${imageName}:${versionTag}"

                        sh """
                            docker tag ${localImage} ${latestTag}
                            docker tag ${localImage} ${versionedTag}
                            docker push ${latestTag}
                            docker push ${versionedTag}
                        """
                    }

                    // Assign image version as an environment variable for later use
                    env.IMAGE_VERSION = versionTag
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-prod', variable: 'KUBECONFIG')]) {
                    script {
                        def ecrUrl = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
                        sh """
                            kubectl apply -f k8s/spring-deployment.yml --validate=false
                            kubectl apply -f k8s/tomcat-service.yml --validate=false

                            kubectl set image deployment/springapp-tomcat-deployment \
                                tomcat=${ecrUrl}/tomcat:${IMAGE_VERSION}

                            kubectl rollout status deployment/springapp-tomcat-deployment
                        """
                    }
                }
            }
        }
    }
}
