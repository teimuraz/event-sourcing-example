val akkaHttpVersion                        = "10.1.13"
val akkaStreamGoogleCloudPubSubGrpcVersion = "2.0.2"
val akkaVersion                            = "2.6.10"
val bcryptVersion                          = "0.4"
val commonCodesVersion                     = "1.15"
val firebaseAdminSdkVersion                = "7.1.0"
val googleCloudPubsubVersion               = "1.112.0"
val jaxbApiVersion                         = "2.3.1"
val jodaConvertVersion                     = "2.0.1"
val logbackVersion                         = "1.2.3"
val logstashEncoderVersion                 = "5.1"
val macwireVersion                         = "2.3.7"
val mockitoVersion                         = "3.2.2.0"
val playJsonJodaVersion                    = "2.8.1"
val playJsonVersion                        = "2.8.1"
val playVersion                            = "2.8.7"
val playJsonDerivedCodesVersion            = "9.0.0"
val reactiveMongoPlayVersion               = "1.0.2-play28"
val reactiveMongoVersion                   = "1.0.2"
val scalaLoggingVersion                    = "3.9.2"
val scalaPbJson4sVersion                   = "0.10.3"
val scalaTestPlusPlayVersion               = "5.0.0"
val scalaTestPlusVersion                   = "3.1.2"
val scalatestVersion                       = "3.2.2"
val tapirVersion                           = "0.17.8"
val testContainersKafkaVersion             = "1.15.1"
val typesafeConfigVersion                  = "1.3.3"

val firebaseAdmin          = "com.google.firebase"         % "firebase-admin"            % firebaseAdminSdkVersion
val googleCloudPubsub      = "com.google.cloud"            % "google-cloud-pubsub"       % googleCloudPubsubVersion
val jaxbApi                = "javax.xml.bind"              % "jaxb-api"                  % jaxbApiVersion
val jbcrypt                = "org.mindrot"                 % "jbcrypt"                   % bcryptVersion
val logbackClassic         = "ch.qos.logback"              % "logback-classic"           % logbackVersion
val logstashLogbackEncoder = "net.logstash.logback"        % "logstash-logback-encoder"  % logstashEncoderVersion
val macwire                = "com.softwaremill.macwire"    %% "macros"                   % macwireVersion % "provided"
val mockito34              = "org.scalatestplus"           %% "mockito-3-4"              % mockitoVersion
val play2Reactivemongo     = "org.reactivemongo"           %% "play2-reactivemongo"      % reactiveMongoPlayVersion
val playJson               = "com.typesafe.play"           %% "play-json"                % playJsonVersion
val playJsonDerivedCodes   = "org.julienrf"                %% "play-json-derived-codecs" % playJsonDerivedCodesVersion
val playLogback            = "com.typesafe.play"           %% "play-logback"             % playVersion
val reactivemongo          = "org.reactivemongo"           %% "reactivemongo"            % reactiveMongoVersion
val scalaLogging           = "com.typesafe.scala-logging"  %% "scala-logging"            % scalaLoggingVersion
val scalatest              = "org.scalatest"               %% "scalatest"                % scalatestVersion
val scalatestFreespec      = "org.scalatest"               %% "scalatest-freespec"       % scalatestVersion
val scalatestplusPlay      = "org.scalatestplus.play"      %% "scalatestplus-play"       % scalaTestPlusPlayVersion
val scalatic               = "org.scalactic"               %% "scalactic"                % scalatestVersion
val tapirCore              = "com.softwaremill.sttp.tapir" %% "tapir-core"               % tapirVersion
val tapirJsonPlay          = "com.softwaremill.sttp.tapir" %% "tapir-json-play"          % tapirVersion
val tapirOpenapiCirceYaml  = "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % tapirVersion
val tapirOpenapiDocs       = "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % tapirVersion
val tapirPlayServer        = "com.softwaremill.sttp.tapir" %% "tapir-play-server"        % tapirVersion
val tapirRedocPlay         = "com.softwaremill.sttp.tapir" %% "tapir-redoc-play"         % tapirVersion
val tapirSwaggerUiPlay     = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-play"    % tapirVersion
val typesafeConfig         = "com.typesafe"                % "config"                    % typesafeConfigVersion

lazy val core = (project in file("lib/core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    name := "lib-core",
    libraryDependencies ++= commonDependencies ++ Seq()
  )

lazy val pubsub = (project in file("lib/pubsub"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "lib-pubsub",
    libraryDependencies ++= commonDependencies ++ Seq(
        googleCloudPubsub
      )
  )

lazy val eventSourcing = (project in file("lib/event-sourcing"))
  .enablePlugins(AutomateHeaderPlugin)
  .dependsOn(core % "compile->compile;test->test", pubsub % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "lib-event-sourcing",
    libraryDependencies ++= commonDependencies ++ Seq(
        )
  )

lazy val api = (project in file("api"))
  .enablePlugins(PlayScala)
  .dependsOn(core % "compile->compile;test->test", eventSourcing)
  .settings(commonSettings)
  .settings(
    name := "api",
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= commonDependencies ++ Seq(
        tapirCore,
        tapirJsonPlay,
        tapirOpenapiCirceYaml,
        tapirOpenapiDocs,
        tapirPlayServer,
        tapirRedocPlay,
        tapirSwaggerUiPlay,
        scalatestplusPlay % Test
      )
  )

lazy val commonDependencies = Seq(
  firebaseAdmin,
  jaxbApi,
  jbcrypt,
  logbackClassic,
  logstashLogbackEncoder,
  macwire,
  play2Reactivemongo,
  playJson,
  playJsonDerivedCodes,
  playLogback,
  reactivemongo,
  typesafeConfig,
  mockito34         % Test,
  scalatest         % Test,
  scalatestFreespec % Test
)

lazy val commonSettings = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  organization := "tk",
  organizationName := "TK",
  startYear := Some(2021),
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.13.3",
  scalafmtOnCompile := true,
  headerLicense := licenseSettings
)

lazy val licenseSettings = Some(
  HeaderLicense.Custom(
    """
     |Copyright 2021 TK
     |
     |This program is free software; you can redistribute it and/or
     |modify it under the terms of the GNU General Public License
     |version 2 as published by the Free Software Foundation.
     |
     |This program is distributed in the hope that it will be useful,
     |but WITHOUT ANY WARRANTY; without even the implied warranty of
     |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     |GNU General Public License for more details.
     |""".stripMargin
  )
)

scalacOptions ++= Seq("-feature")
