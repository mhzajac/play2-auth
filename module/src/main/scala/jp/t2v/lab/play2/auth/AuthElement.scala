package jp.t2v.lab.play2.auth

import javax.inject._
import play.api.mvc._
import com.jaroop.play.stackc.{RequestWithAttributes, RequestAttributeKey, StackableController}
import scala.concurrent.{ ExecutionContext, Future }
import scala.reflect._

trait Env {

    type Id

    type User

    type Authority

}

class OptionalAuthRequest[A, User](request: Request[A], val user: Option[User]) extends WrappedRequest[A](request)

@Singleton
class OptionalAuthenticatedActionBuilder[E <: Env] @Inject() (
  val parser: BodyParsers.Default,
  val config: AuthConfig[E],
  val idContainer: IdContainer[E#Id],
  val tokenAccessor: TokenAccessor
)(implicit val executionContext: ExecutionContext) extends ActionBuilder[({type L[X] = OptionalAuthRequest[X, E#User]})#L, AnyContent]
    with AsyncAuth[E] {

  val asyncIdContainer = AsyncIdContainer(idContainer)

  override def invokeBlock[A](request: Request[A], block: OptionalAuthRequest[A, E#User] => Future[Result]) = {
    implicit val r = request
    val maybeUserFuture = restoreUser.recover { case _ => None -> identity[Result] _ }
    maybeUserFuture.flatMap { case (maybeUser, cookieUpdater) =>
      block(new OptionalAuthRequest(request, maybeUser)).map(cookieUpdater)
    }
  }
}

class AuthRequest[A, User](request: Request[A], val user: User) extends WrappedRequest[A](request)

@Singleton
class AuthenticatedActionBuilder[E <: Env] @Inject() (
  val parser: BodyParsers.Default,
  val config: AuthConfig[E],
  val idContainer: IdContainer[E#Id],
  val tokenAccessor: TokenAccessor
)(implicit val executionContext: ExecutionContext) extends ActionBuilder[({type L[X] = AuthRequest[X, E#User]})#L, AnyContent]
    with AsyncAuth[E] { self =>

  val asyncIdContainer = AsyncIdContainer(idContainer)

  final def withAuthorization(authority: E#Authority): ActionBuilder[({type L[X] = AuthRequest[X, E#User]})#L, AnyContent] = new ActionBuilder[({type L[X] = AuthRequest[X, E#User]})#L, AnyContent] {
    override def parser = self.parser
    override protected implicit def executionContext = self.executionContext
    override protected def composeParser[A](bodyParser: BodyParser[A]): BodyParser[A] = self.composeParser(bodyParser)
    override protected def composeAction[A](action: Action[A]): Action[A] = self.composeAction(action)

    override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]) = {
      implicit val r = request
      authorized(authority) flatMap {
        case Right((user, resultUpdater)) => block(new AuthRequest(request, user)).map(resultUpdater)
        case Left(result) => Future.successful(result)
      }
    }
  }

  override def invokeBlock[A](request: Request[A], block: AuthRequest[A, E#User] => Future[Result]) = {
    implicit val r = request

    restoreUser recover {
      case _ => None -> identity[Result] _
    } flatMap {
      case (Some(user), cookieUpdater) => block(new AuthRequest(request, user)).map(cookieUpdater)
      case (None, _) => config.authenticationFailed(request)
    }
  }

}
