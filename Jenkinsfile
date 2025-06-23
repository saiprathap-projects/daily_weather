pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = '340752824368'
        ECR_REPO = 'tomcat'
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir()
            }
        }

        stage('Clone Repository') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/saiprathap-projects/daily_weather.git',
                    credentialsId: 'git-cred'
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
                withCredentials([usernamePassword(credentialsId: 'aws-creds', usernameVariable: 'AWS_ACCESS_KEY_ID', passwordVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    script {
                        def repoExists = sh(
                            script: "aws ecr describe-repositories --repository-names ${env.ECR_REPO} --region ${env.AWS_REGION}",
                            returnStatus: true
                        )

                        if (repoExists != 0) {
                            dir('terraform/ECR') {
                                sh '''
                                    terraform init
                                    terraform apply -auto-approve
                                '''
                            }
                        } else {
                            echo "ECR repository already exists. Skipping Terraform."
                        }
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $ECR_REPO:latest .'
            }
        }

        stage('Tag & Push Image to ECR') {
            steps {
                script {
                    def ecrUrl = "${env.AWS_ACCOUNT_ID}.dkr.ecr.${env.AWS_REGION}.amazonaws.com"
                    def commitId = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    def versionTag = "v${env.BUILD_NUMBER}-${commitId}"

                    sh """
                        docker tag ${ECR_REPO}:latest ${ecrUrl}/${ECR_REPO}:latest
                        docker tag ${ECR_REPO}:latest ${ecrUrl}/${ECR_REPO}:${versionTag}
                        docker push ${ecrUrl}/${ECR_REPO}:latest
                        docker push ${ecrUrl}/${ECR_REPO}:${versionTag}
                    """

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
                            kubectl apply -f k8s/tomcat-deployment.yml --validate=false
                            kubectl apply -f k8s/tomcat-service.yml --validate=false

                            kubectl set image deployment/springapp-tomcat-deployment \
                                tomcat=${ecrUrl}/${ECR_REPO}:${IMAGE_VERSION}

                            kubectl rollout status deployment/springapp-tomcat-deployment
                        """
                    }
                }
            }
        }
    }
}
