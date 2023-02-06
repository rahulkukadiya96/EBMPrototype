package exception

case class AuthenticationException(message: String) extends Exception(message)
