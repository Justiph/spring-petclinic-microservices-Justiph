pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    // Xác định service thay đổi dựa trên thư mục
                    def changedFiles = sh(script: 'git diff --name-only HEAD HEAD~1', returnStdout: true).split('\n')
                    def servicesToBuild = determineServices(changedFiles)
                    
                    servicesToBuild.each { service ->
                        buildService(service)
                    }
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    servicesToBuild.each { service ->
                        runTests(service)
                    }
                }
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/src/test*'
                    )
                }
            }
        }
    }
    
    post {
        failure {
            emailext body: 'Build failed!', subject: 'Build failed', to: 'phantaihbl9102018@gmail.com'
        }
    }
}

def determineServices(changedFiles) {
    def services = []
    changedFiles.each { file ->
        if (file.startsWith('spring-petclinic-customers-service/')) {
            services << 'customers-service'
        }
        // Thêm các service khác tương tự
    }
    return services.unique()
}

def buildService(service) {
    dir("spring-petclinic-${service}") {
        sh './mvnw clean package -DskipTests'
    }
}

def runTests(service) {
    dir("spring-petclinic-${service}") {
        sh './mvnw test'
    }
}