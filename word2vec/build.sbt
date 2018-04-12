name := "word2vec"

version := "0.1"

scalaVersion := "2.12.5"

enablePlugins(JavaAppPackaging)

val deeplearningVersion = "0.9.1"

val nd4jVersion = "0.9.1"

libraryDependencies ++= Seq(
  "org.deeplearning4j" % "deeplearning4j-core" % deeplearningVersion,
  "org.deeplearning4j" % "deeplearning4j-nlp" % deeplearningVersion,
  "org.nd4j" % "nd4j-native-platform" % nd4jVersion,
  "org.nd4j" % "nd4j-native" % nd4jVersion classifier "macosx-x86_64-avx2",
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe" % "config" % "1.3.2",
  "org.slf4j" % "slf4j-simple" % "1.7.25"
)
