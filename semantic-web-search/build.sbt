val scalaV              = "2.12.4"
val deeplearningVersion = "0.9.1"
val akkaVersion         = "10.0.10"
val monixVersion        = "3.0.0-RC1"

lazy val server = (project in file("server"))
  .settings(
    scalaVersion := scalaV,
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    // triggers scalaJSPipeline when using compile or continuous compilation
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      "com.typesafe.akka"  %% "akka-http"            % akkaVersion,
      "com.typesafe.akka"  %% "akka-http-spray-json" % akkaVersion,
      "com.vmunier"        %% "scalajs-scripts"      % "1.1.2",
      "org.deeplearning4j" % "deeplearning4j-core"   % deeplearningVersion,
      "org.deeplearning4j" % "deeplearning4j-nlp"    % deeplearningVersion,
      "org.deeplearning4j" % "deeplearning4j-zoo"    % deeplearningVersion,
      "org.nd4j"           % "nd4j-native-platform"  % deeplearningVersion,
      "org.nd4j"           % "nd4j-native"           % deeplearningVersion classifier "macosx-x86_64-avx2",
      "org.slf4j"      % "slf4j-simple"       % "1.7.25",
      "com.drewnoakes" % "metadata-extractor" % "2.11.0",
      "io.monix"       %% "monix"             % monixVersion
    ),
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value
  )
  .enablePlugins(SbtWeb, JavaAppPackaging)
  .dependsOn(sharedJvm)

lazy val client = (project in file("client"))
  .settings(
    scalaVersion := scalaV,
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.5",
      "io.monix"     %%% "monix"       % monixVersion,
      "com.lihaoyi"  %%% "upickle"     % "0.6.5"
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)

lazy val shared =
  (crossProject.crossType(CrossType.Pure) in file("shared")).settings(
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.6.7"
    )
  )

lazy val sharedJvm = shared.jvm
lazy val sharedJs  = shared.js

// loads the server project at sbt startup
onLoad in Global := (onLoad in Global).value andThen { s: State =>
  "project server" :: s
}
