package jp.t2v.lab.play2.auth

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.RequestHeader
import scala.reflect._

trait AsyncIdContainer[Id] {

  def startNewSession(userId: Id, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[AuthenticityToken]

  def remove(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit]

  def get(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Option[Id]]

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[Unit]

}

object AsyncIdContainer {

  def apply[Id](underlying: IdContainer[Id]): AsyncIdContainer[Id] = new AsyncIdContainer[Id] {

    def startNewSession(userId: Id, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[AuthenticityToken] =
      Future.successful(underlying.startNewSession(userId, timeoutInSeconds))

    def remove(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Unit] =
      Future.successful(underlying.remove(token))

    def get(token: AuthenticityToken)(implicit context: ExecutionContext): Future[Option[Id]] =
      Future.successful(underlying.get(token))

    def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int)(implicit request: RequestHeader, context: ExecutionContext): Future[Unit] =
      Future.successful(underlying.prolongTimeout(token, timeoutInSeconds))

  }

}
