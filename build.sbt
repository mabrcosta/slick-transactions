name := "slick-transactions"
version := "0.1"
scalaVersion := "2.12.4"

libraryDependencies ++= {
  val slickV = "3.2.2"
  Seq(
    "com.typesafe.slick"            %%  "slick"                       % slickV,
    "com.h2database"                %   "h2"                          % "1.4.197"   % Test,
    "ch.qos.logback"                %   "logback-classic"             % "1.2.3"     % Test,
    "org.scalatest"                 %%  "scalatest"                   % "3.0.5"     % Test,
    "org.mockito"                   %   "mockito-core"                % "2.16.0"    % Test
  )
}