// CI for Jenkins Build Metrics Plugin
@Library("PSL") _



properties([parameters([
        booleanParam(name: 'prepareRelease', defaultValue: false, description: 'Prepare for release?'),
        string(name: 'overrideVersionNum', defaultValue: '', description: 'Override version number?'),
        string(name: 'devVersionNum', defaultValue: '', description: 'Override development version number?')
])])

node('aws-centos') {

    checkout([
            $class: 'GitSCM',
            branches: scm.branches,
            doGenerateSubmoduleConfigurations: scm.doGenerateSubmoduleConfigurations,
            extensions: scm.extensions+[[$class: 'LocalBranch']],
            userRemoteConfigs: scm.userRemoteConfigs
    ])
    // Getting artifact name and version number from maven pom.xml
    def pom = readMavenPom()
    String artifactId = pom.getArtifactId()
    String versionNum = pom.getVersion()
    def dockerImage = docker.image("artifactory.dev.adskengineer.net/hardened-build/maven")

    stage('Setup') {
        dockerImage.pull()
    }

    dockerImage.inside('-u root') {
        stage('Build/Release') {

            if (params.prepareRelease == true) {  // Performing maven release and upload to artifactory

                // Git is required for releases
                sh "apk add git"

                String releaseParams = ""
                if (versionNum.contains("-SNAPSHOT")) {
                    versionNum = versionNum - "-SNAPSHOT"
                }
                if (params.overrideVersionNum != null && params.overrideVersionNum != "") {
                    versionNum = params.overrideVersionNum
                    // checking if params.nextVersionNum is set
                    try {
                        assert params.devVersionNum != null && params.devVersionNum != ""
                    } catch (AssertionError e) {
                        error "Please set the development version if you wish to override the current version number."
                    }
                    releaseParams += "-DreleaseVersion=${params.overrideVersionNum} -DdevelopmentVersion=${params.devVersionNum}"
                }

                // perform maven release - creates tags and prepares the pom for the development of the next version
                withGit {  // usage of withGit to setup git to use svc_p_ors git credentials
                    sh "git config user.email \"orchestration.solutions@autodesk.com\""
                    sh "git config user.name \"svc_p_ors\""
                    sh "mvn -B release:clean release:prepare ${releaseParams}"
                }
                uploadToArtifactory('local-svc_p_ors_art', artifactId, versionNum)

            } else {  // Building and testing
                withCredentials([string(credentialsId: 'aws_insights_api_key', variable: 'apiKey'), usernamePassword(credentialsId: 'AthenaElasticSearch', passwordVariable: 'password', usernameVariable: 'username')]) {
                    sh "mvn clean install -B"
                }
            }
        }
    }

}


def uploadToArtifactory(String credentialsId, String artifactId, String versionNum) {

    dir ('target') {

        def commonArtifactory = new ors.utils.CommonArtifactory(steps, env, Artifactory, credentialsId)
        echo "Files in target folder"
        sh "ls -la"
        println "Artifact ID: ${artifactId}, version: ${versionNum}"

        String artifactoryPath = "team-fcia-generic/jenkins/plugins/${artifactId}"
        String uploadSpec = """{
                                        "files": [
                                            {
                                                "pattern": "${artifactId}.hpi",
                                                "target": "${artifactoryPath}/${versionNum}/"
                                            }
                                        ]
                                    }"""
        commonArtifactory.upload('https://art-bobcat.autodesk.com/artifactory/', uploadSpec)
    }
}