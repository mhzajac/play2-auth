package jp.t2v.lab.play2.auth

import javax.inject.Inject
import play.api.mvc._
import play.api.mvc.Cookie
import play.api.libs.Crypto
import scala.concurrent.{Future, ExecutionContext}

class Login[E <: Env] @Inject() (
  config: AuthConfig[E],
  idContainer: IdContainer[E#Id],
  tokenAccessor: TokenAccessor
) {

  val asyncIdContainer = AsyncIdContainer(idContainer)

  def gotoLoginSucceeded(userId: E#Id)(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    gotoLoginSucceeded(userId, config.loginSucceeded(request))
  }

  def gotoLoginSucceeded(userId: E#Id, result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = for {
    token <- asyncIdContainer.startNewSession(userId, config.sessionTimeoutInSeconds)
    r     <- result
  } yield tokenAccessor.put(token)(r)
}

class Logout[E <: Env] @Inject() (
  config: AuthConfig[E],
  idContainer: IdContainer[E#Id],
  tokenAccessor: TokenAccessor
) {

  val asyncIdContainer = AsyncIdContainer(idContainer)

  def gotoLogoutSucceeded(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    gotoLogoutSucceeded(config.logoutSucceeded(request))
  }

  def gotoLogoutSucceeded(result: => Future[Result])(implicit request: RequestHeader, ctx: ExecutionContext): Future[Result] = {
    tokenAccessor.extract(request) foreach asyncIdContainer.remove
    result.map(tokenAccessor.delete)
  }
}

// trait LoginLogout extends Login with Logout {
//   self: Controller with AuthConfig =>
// }
