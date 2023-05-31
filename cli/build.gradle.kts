plugins { application }
application.mainClass.set("org.aya.anqur.cli.CliMain")

dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.antlr", "antlr4-runtime", version = deps.getProperty("version.antlr"))
  api("info.picocli", "picocli", version = deps.getProperty("version.picocli"))
  implementation(project(":base"))
  implementation("org.aya-prover", "tools-repl", version = deps.getProperty("version.aya"))
}

val genDir = "src/main/gen"
sourceSets["main"].java.srcDir(file(genDir))
idea.module {
  sourceDirs.add(file(genDir))
}

val genVer = tasks.register<GenerateVersionTask>("genVer") {
  basePackage = "org.aya.anqur"
  outputDir = file(genDir).resolve("org/aya/anqur/prelude")
}
listOf(tasks.sourcesJar, tasks.compileJava).forEach { it.configure { dependsOn(genVer) } }
