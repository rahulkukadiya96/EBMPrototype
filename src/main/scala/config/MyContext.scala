package config

import dao.AppDAO
import exception.{AuthenticationException, AuthorizationException}
import models.Patient

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Inf

case class MyContext(dao: AppDAO, currentUser: Option[Patient] = None) {
  /*def login(email: String, password: String): Patient = {
    val userOpt = Await result(dao.authenticate(email, password), Inf)
    userOpt.getOrElse(
      throw AuthenticationException("email or password are incorrect!")
    )
  }*/

  def ensureAuthenticated(): Any = currentUser.isEmpty match {
    case true => throw AuthorizationException("You do not have permission. Please sign in.")
    case false => println("User is already logged in")
  }
}