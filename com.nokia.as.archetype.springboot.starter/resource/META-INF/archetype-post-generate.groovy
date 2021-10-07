def moduleDir = new File(request.getOutputDirectory() + "/" + request.getArtifactId())
def springFactories = new File(request.getOutputDirectory() + "/" + request.getArtifactId() + "/src/main/resources/META-INF")  
def bundlesMarker = "\$BUNDLE-ARTIFACTS\$" 
def depsMarker = "\$BUNDLE-DEPENDENCIES\$" 
def embeddableMarker = "\$EMBEDDABLE-API\$" 

def pomFile = new File(moduleDir, "pom.xml")
def factoriesFile = new File(springFactories, "spring.factories")
def pomContent = pomFile.getText("UTF-8")
def factoriesContent = factoriesFile.getText("UTF-8")
def tempDir = new File("/tmp")  

new ProcessBuilder("wget", "-O", "com.nokia.casr.microfeatures.main.jar", "http://repo.lab.pl.alcatel-lucent.com/csf-mvn-delivered/com/nokia/casr/microfeatures/com.nokia.casr.microfeatures.main/[RELEASE]/com.nokia.casr.microfeatures.main-[RELEASE].jar")
    .redirectErrorStream(true)
    .directory(tempDir)
    .start()
    .inputStream.eachLine {println it} 

println "Using obr: " + obr
ProcessBuilder pb 
if(obr.equals("local")) {
    pb = new ProcessBuilder("java", "-Dm2", "-Dcreate=test,1.0.0," + features, "-jar", "com.nokia.casr.microfeatures.main.jar")
} else if(!obr.equals("latest")) {
    pb = new ProcessBuilder("java", "-Dobr=" + obr, "-Dcreate=test,1.0.0," + features, "-jar", "com.nokia.casr.microfeatures.main.jar")
} else {
    pb = new ProcessBuilder("java", "-Dcreate=test,1.0.0," + features, "-jar", "com.nokia.casr.microfeatures.main.jar")
}

pb.redirectErrorStream(true)
  .directory(tempDir)
  .start()
  .inputStream.eachLine {println it} 

new ProcessBuilder("rm", "-rf", "test-1.0.0")
    .directory(tempDir)
    .start()
    .waitFor()

new ProcessBuilder("unzip", "-o", "test-1.0.0.zip")
    .redirectErrorStream(true)
    .directory(tempDir)
    .start()
    .inputStream.eachLine {println it} 
    
println "Generating starter CASR dependencies ..."
def artifacts = "/tmp/test-1.0.0/scripts/getArtifacts.sh -m".execute().text  
println artifacts 
pomContent = pomContent.replace(bundlesMarker, artifacts) 

println "Generating starter API dependencies ..."
def deps = "/tmp/test-1.0.0/scripts/getArtifacts.sh -a -m".execute().text  
println deps 
pomContent = pomContent.replace(depsMarker, deps)

println "Generating starter API Bundle Symbolic Names ..."
def apis = "/tmp/test-1.0.0/scripts/getArtifacts.sh -a -bsn".execute().text
def result = String.join(",", Arrays.asList(apis.split("\n")))
println result
pomContent = pomContent.replace(embeddableMarker, result)

factoriesContent.replace("\${artifactId}", artifactId)

pomFile.newWriter().withWriter { w ->
    w << pomContent
}

factoriesFile.newWriter().withWriter { w ->
    w << factoriesContent
}
