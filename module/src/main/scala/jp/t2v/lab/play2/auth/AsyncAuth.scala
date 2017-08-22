package jp.t2v.lab.play2.auth

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

trait AsyncAuth[E <: Env] {

  def config: AuthConfig[E]

  def asyncIdContainer: AsyncIdContainer[E#Id]

  def tokenAccessor: TokenAccessor

  def authorized(authority: E#Authority)(implicit request: RequestHeader, context: ExecutionContext): Future[Either[Result, (E#User, ResultUpdater)]] = {
    restoreUser collect {
      case (Some(user), resultUpdater) => Right(user -> resultUpdater)
    } recoverWith {
      case _ => config.authenticationFailed(request).map(Left.apply)
    } flatMap {
      case Right((user, resultUpdater)) => config.authorize(user, authority) collect {
        case true => Right(user -> resultUpdater)
      } recoverWith {
        case _ => config.authorizationFailed(request, user, Some(authority)).map(Left.apply)
      }
      case Left(result) => Future.successful(Left(result))
    }
  }

  private[auth] def restoreUser(implicit request: RequestHeader, context: ExecutionContext): Future[(Option[E#User], ResultUpdater)] = {
    (for {
      token  <- extractToken(request)
    } yield for {
      Some(userId) <- asyncIdContainer.get(token)
      Some(user)   <- config.resolveUser(userId)
      _            <- asyncIdContainer.prolongTimeout(token, config.sessionTimeoutInSeconds)
    } yield {
      Option(user) -> tokenAccessor.put(token) _
    }) getOrElse {
      Future.successful(Option.empty -> identity)
    }
  }

  private[auth] def extractToken(request: RequestHeader): Option[AuthenticityToken] = {
    if (play.api.Play.maybeApplication.forall(_.mode == play.api.Mode.Test)) {
      request.headers.get("PLAY2_AUTH_TEST_TOKEN") orElse tokenAccessor.extract(request)
    } else {
      tokenAccessor.extract(request)
    }
  }

}
