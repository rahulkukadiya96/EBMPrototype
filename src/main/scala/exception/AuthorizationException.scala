package exception

case class AuthorizationException(message: String) extends Exception(message)
