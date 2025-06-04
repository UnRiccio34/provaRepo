/***************************************************
 * Support team: acn-hsc-devops@accenture.com
 ***************************************************
 * Pipeline dedicata al Deploy dei servizi relativi a SAC
 * Prevede i seguenti stages:
 * 1 - Preliminary setup
 * 2 - Helm Deploy
 * 3 - Kubernetes Check
 ***************************************************/
 
import com.accenture.ies.devops.mds.utilities.ContextRegistry
import com.accenture.ies.devops.mds.managers.Common
import com.accenture.ies.devops.mds.managers.Kubernetes
import com.accenture.ies.devops.mds.managers.Gitlab
import com.accenture.ies.devops.mds.managers.Helm
import com.accenture.ies.devops.mds.utilities.IStepExecutor
import com.accenture.ies.devops.mds.managers.Versioning
 
 
 
def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()
 
    steps.echo "Pipeline Params: ${pipelineParams}"
   
    /*
    * Blocco variabili globali
    */
    def configs, applicationConfigs, workspace_path
    def gitlabURL, gitlabTOKEN
    def repoURL, nexusURL, nexusHelmURL
    def valuesYAML, namespace, jenkins_baseURL, cd
 
    /*
    * Blocco istanze globali
    */
    IStepExecutor stepExecutor
    Common common
    Kubernetes k8s
    Gitlab gitlab
    Helm helm
    Versioning versioning
 
    /*
    * Blocco parametri di input
    */
    def component = "${params.component}"
    def componentVersion = "${params.componentVersion}"
    def imageVersion = "${params.imageVersion}"
    def ambienteDestinazione = "${params.ambienteDestinazione}"
 
 
 
    pipeline {
        parameters {
            string(name: 'component', description: 'Inserire il nome del repository GitLab')
            string(name: 'componentVersion', description: 'componentVersion')
            // RESTList(name: 'imageVersion',
            //             description: 'Scegliere attentamente la versione da installare',
            //             restEndpoint: 'http://10.64.102.130:8082/service/rest/v1/search?repository=docker-images&name=docker-images%2Fhelloworld',
            //             credentialId: 'NEXUS_CREDS',
            //             mimeType: 'APPLICATION_JSON',
            //             valueExpression: '.items.*.version',
            //             valueOrder: 'DSC'
            //         )
            choice(name: 'ambienteDestinazione', choices: ['sviluppo', 'collaudo', 'pre-produzione', 'produzione'], description: 'Selezionare l\'ambiente di destinazione')
        }
   
        agent {
            label "${pipelineParams.node}"
        }
 
        options {
            disableConcurrentBuilds()
            timeout(time: 5, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '30'))
            ansiColor('xterm')
        }
 
        environment {
            APPLICATION_CONFIG_FILE = "pipeline_configs.yaml"
            APPLICATION_VERSIONS_FILE = "software_versions.json"
 
        }
 
        stages {
            stage ("Preliminary setup") {
                steps {
                    script {
                        ansiColor('xterm') {
                            message("Preliminary Setup")
 
                       
                            ContextRegistry.registerDefaultContext(this)
                            stepExecutor = ContextRegistry.getContext().getStepExecutor()
                            common = new Common(stepExecutor._steps)
                            helm = new Helm(stepExecutor._steps)
                            k8s = new Kubernetes(stepExecutor._steps)
 
                            configs = ContextRegistry.getContext().getPipelineConfigs()
                            workspace_path = "${WORKSPACE}"
                            gitlabURL = "${configs.tools.gitlab.url}"
                            gitlabTOKEN = "${configs.tools.gitlab.token}"
                            jenkins_baseURL = "${configs.tools.jenkins.url}"
                            gitlab = new Gitlab(stepExecutor._steps, gitlabURL, gitlabTOKEN, pipelineParams.node)
 
                            ContextRegistry.getContext().setApplicationConfigs(readYaml(file: "${WORKSPACE}/${APPLICATION_CONFIG_FILE}"))
                            softwareVersions = readJSON file: "${WORKSPACE}/${APPLICATION_VERSIONS_FILE}"
                       
                            applicationConfigs = ContextRegistry.getContext().getApplicationConfigs()
                            nexusURL = "${configs.tools.nexus.url}"                        
                            nexusHelmURL = "${applicationConfigs.repos.nexus.helm}"
                            cd = "${applicationConfigs.cd}".toBoolean()
                            echo "Component Version ---> ${componentVersion}"
                            if (ambienteDestinazione.equalsIgnoreCase("sviluppo")) {
                                valuesYAML = "sviluppo"
                                namespace = "${applicationConfigs.ocp.namespaces.sviluppo}"
                            } else if (ambienteDestinazione.equalsIgnoreCase("collaudo")) {
                                valuesYAML = "collaudo"
                                namespace = "${applicationConfigs.ocp.namespaces.collaudo}"
                            } else if (ambienteDestinazione.equalsIgnoreCase("pre-produzione")) {
                                valuesYAML = "pre-produzione"
                                namespace = "${applicationConfigs.ocp.namespaces.pre-produzione}"
                            } else if (ambienteDestinazione.equalsIgnoreCase("produzione")) {
                                valuesYAML = "produzione"
                                namespace = "${applicationConfigs.ocp.namespaces.produzione}"
                            }
 
                            chartName = "${component}-chart"
                            chartVersion = "1.0.0"
 
                            echo "Preliminary Setup Result: ${currentBuild.currentResult}"
                        }
                    }
                }
            }
 
            stage ("Helm Deploy") {
                steps {
                    script {
                        ansiColor('xterm') {
                            message("Helm Preconditions")
 
                            // helm.repoAdd(component, nexusURL, nexusHelmURL)
                            // helm.repoFetch(component, chartName, chartVersion)
                            // helm.repoUpdate(component)
                            // helm.deploy(workspace_path, namespace, component, chartName, chartVersion, valuesYAML)
 
                            echo "Helm Deploy Result: ${currentBuild.currentResult}"
                        }
                    }
                }
            }
     
            stage ("Kubernetes Check") {
                steps {
                    script {
                        ansiColor('xterm') {
                            message("Kubernetes Check")
                            // k8s.restartDeployment(component, namespace)
                            // k8s.statusDeployment(component, namespace)
 
                            //Mi istanzio una variabile per fare un check sullo step
                            def kubCheck = currentBuild.currentResult
                            echo "Kubernetes Check Result: ${currentBuild.currentResult}"
                        }
                    }      
                }
            }
       
        }          
 
        post {
            success {
                script {
                    ansiColor('xterm') {
                        message("SUCCESS Post Function")
 
                            echo "SUCCESS Post Function"
 
                            //possibilità di invocare test Jmeter o Selenium
                            //richiamo automatico del job con innesco parametri.
                            //Se la build del MS è andata a buon fine e CD è true parte in automatico l invocazione della deploy che porta il ms in Svil
                            //aggiungere booleano controllo versioning true false
                            if (cd && ambienteDestinazione.equalsIgnoreCase("sviluppo")){
                               
                                boolean checkBranch = gitlab.checkStagingExistance()
 
                                echo "Il ritorno del metodo è ${checkBranch}"
                                if (checkBranch) {
 
                                    echo "Il branch Staging esiste, proseguo con le operazioni"
 
                                } else {
 
                                    echo "Il branch Staging non esiste, lo creo."
                                    gitlab.createStagingBranch(component, workspace_path)
 
                                    // Check su tag,
                                    // legge la versione presente in svil.version senza -Snapshot
                                    // controlla se esiste il tag con la stessa versione e -RC1
                                    // non esiste, quindi crea tag alla stessa versione di sviluppo
                                }
                                    echo "Controllo l esistenza del tag."
                                    def componentVersion_collaudo = "${softwareVersions.coll.version}"
                                    boolean checkTag = gitlab.checkCollTagExistance(componentVersion_collaudo)
                                    echo "Il ritorno del metodo tag è -----------------------------> ${checkTag}"
 
                                    //sleep 5000
                                    if (checkTag) {
                                   
                                   
                                    //se il tag gia esiste? mando in errore il triggher.
                                    error ("FATAL - IL TAG ESISTE, CONTROLLARE IL VERSIONAMENTO.")
 
                                    }else{
                                       
 
                                        echo "Il Tag non esiste. Lo creo."
 
                                        gitlab.createTagNoApiColl(workspace_path, component, componentVersion_collaudo)
 
                                    }
                                   
                                    //Continous Deployment non pensata per progetti che non seguono versioning tramite file
                                } else if (cd && ambienteDestinazione.equalsIgnoreCase("collaudo")){
 
                                    boolean checkBranch = gitlab.checkProdExistance()
                                    echo "Il ritorno del metodo è ${checkBranch}"
 
                                    if (checkBranch) {
 
                                         echo "Il branch Produzione esiste, proseguo con le operazioni"
 
                                    } else {
 
                                        echo "Il branch Staging non esiste, lo creo."
                                        gitlab.createProdBranch(component, workspace_path)
 
 
                                    }
 
                                    echo "Controllo l esistenza del tag."
                                    def componentVersion_produzione = "${softwareVersions.prod.version}"
                                    boolean checkTag = gitlab.checkTagExistance(componentVersion_produzione)
                                    echo "Il ritorno del metodo tag è -----------------------------> ${checkTag}"
 
                                    if (checkTag) {
                                   
                                   
                                    //se il tag gia esiste? mando in errore il triggher.
                                    error ("FATAL - IL TAG ESISTE, CONTROLLARE IL VERSIONAMENTO.")
 
                                    }else{
                                       
 
                                        echo "Il Tag non esiste. Lo creo."
 
                                        gitlab.createTagNoApiProd(workspace_path, component, componentVersion_produzione)
 
                                    }
 
 
                                }
                                    cleanWs()
 
                            }
                }
            }
 
            failure {
                script {
                    ansiColor('xterm') {
                        message("FAILURE Post Function")
 
                       /* if(kubCheck.equalsIgnoreCase("FAILED")) {
                            //Recuperare la url e le credenziali per fare il log in al docker registry
                            k8s.rollback(component, namespace, registryUser, registryUrl, registryPassword)
                        } */
                        echo "FAILURE Post Function"
                        cleanWs()
                    }
                }
            }
 
            aborted {
                script {
                    ansiColor('xterm') {
                        message("ABORT Post Function")
 
                            echo "ABORT Post Function"
                            cleanWs()
                           
                    }
                }
            }
        }
    }
}
 
