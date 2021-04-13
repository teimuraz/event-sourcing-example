package domain.systemmanager

import domain.systemmanager.SystemManagerRole.Admin

sealed trait SystemManagerRole {
  def stringValue: String = this match {
    case Admin => "ADMIN"
  }

}

object SystemManagerRole {
  case object Admin extends SystemManagerRole

  def apply(value: String): Either[String, SystemManagerRole] =
    value.trim.toUpperCase match {
      case "ADMIN" => Right(SystemManagerRole.Admin)
      case unknown => Left(s"Unknown role $unknown")
    }

}
