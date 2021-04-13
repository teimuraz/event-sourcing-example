addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.7")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                  % "0.9.3")
addSbtPlugin("de.heikoseeberger"  % "sbt-header"               % "3.0.2")
addSbtPlugin("org.scalameta"            % "sbt-scalafmt"        % "2.2.1")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"      % "1.3.4")
addSbtPlugin("com.github.gseitz"  % "sbt-release"              % "1.0.8")
addSbtPlugin("io.spray"           % "sbt-revolver"             % "0.9.1")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")


libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25" // Needed by sbt-git
