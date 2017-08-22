package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.reflect.{ClassTag, classTag}
import scala.concurrent.{ExecutionContext, Future}

trait AuthConfig[E <: Env] {

  def sessionTimeoutInSeconds: Int

  def resolveUser(id: E#Id)(implicit context: ExecutionContext): Future[Option[E#User]]

  def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result]

  def authorizationFailed(request: RequestHeader, user: E#User, authority: Option[E#Authority])(implicit context: ExecutionContext): Future[Result]

  def authorize(user: E#User, authority: E#Authority)(implicit context: ExecutionContext): Future[Boolean]

}
