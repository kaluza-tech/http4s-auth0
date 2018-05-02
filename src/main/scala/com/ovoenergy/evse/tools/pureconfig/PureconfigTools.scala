package com.ovoenergy.evse.tools.pureconfig

import org.http4s.{ParseFailure, Uri}
import pureconfig._
import pureconfig.error.{CannotConvert, ConfigReaderFailure, ConfigValueLocation}

object PureconfigTools {
  private def toConfigReaderFailure(string: String, tpe: String, location: Option[ConfigValueLocation])(pf: ParseFailure): ConfigReaderFailure =
    CannotConvert(string, tpe, pf.details, location, "")

  implicit val myUriReader: ConfigReader[Uri] = ConfigReader.fromString[Uri](
    string => location => Uri.fromString(string).left.map(toConfigReaderFailure(string, "Uri", location)))

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, KebabCase))
}
