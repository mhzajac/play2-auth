package controllers

import javax.inject._
import jp.t2v.lab.play2.auth._
import play.api._
import play.api.mvc._, Results._
import scala.concurrent._
import scala.reflect._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (
  authenticatedAction: AuthenticatedActionBuilder[MyEnv],
  optionalAuthAction: OptionalAuthenticatedActionBuilder[MyEnv],
  loginService: Login[MyEnv],
  logoutService: Logout[MyEnv],
  us: UserService,
  ec: ExecutionContext
) extends InjectedController {

  implicit val exc = ec

  def index = optionalAuthAction { implicit request =>
    val msg = request.user.map(user => s"You are logged in as: $user").getOrElse("You are not logged in.")
    Ok(views.html.index(s"Authentication on this page is optional: $msg"))
  }

  def login(id: Long) = Action.async { implicit request =>
    us.read(id).map(user => loginService.gotoLoginSucceeded(id))
      .getOrElse(Future.successful(NotFound("Invalid credentials.")))
  }

  def logout() = Action.async { implicit request =>
    logoutService.gotoLogoutSucceeded(Future.successful(Redirect("/")))
  }

  def priv = authenticatedAction { implicit request =>
    Ok(views.html.index(s"Success! You are logged in as ${request.user}"))
  }

  def lockedView = authenticatedAction.withAuthorization(View) { implicit request =>
    Ok(views.html.index(s"You have the required permissions (View) to see this page, and are logged in as: ${request.user}"))
  }

  def lockedList = authenticatedAction.withAuthorization(List) { implicit request =>
    Ok(views.html.index(s"You have the required permissions (List) to see this page, and are logged in as: ${request.user}"))
  }
}

sealed trait Permission

case object View extends Permission
case object List extends Permission

class LongId extends IdType[Long] {
    val ct = classTag[Long]
}

trait MyEnv extends Env {
    type Id = Long
    type User = _root_.controllers.User
    type Authority = Permission
}

class MyAuthConfig @Inject() (ps: PermissionService, us: UserService) extends AuthConfig[MyEnv] {

  def sessionTimeoutInSeconds: Int = 120

  def resolveUser(id: Long)(implicit context: ExecutionContext): Future[Option[User]] = Future.successful(us.read(id))

  def loginSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
    Future.successful(Redirect("/"))

  def logoutSucceeded(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
    Future.successful(Redirect("/"))

  def authenticationFailed(request: RequestHeader)(implicit context: ExecutionContext): Future[Result] =
    Future.successful(Forbidden("You are not logged in."))

  def authorizationFailed(request: RequestHeader, user: User, authority: Option[Permission])(implicit context: ExecutionContext): Future[Result] =
    Future.successful(Forbidden("Unauthorized."))

  def authorize(user: User, authority: Permission)(implicit context: ExecutionContext): Future[Boolean] =
    Future.successful(ps.isAuthorized(user, authority))

}

case class User(id: Option[Long], email: String)

class UserService {

    def read(id: Long): Option[User] = id match {
        case 1L => Option(User(Option(1L), "test@jaroop.com"))
        case 2L => Option(User(Option(2L), "bob@jaroop.com"))
        case _ => None
    }

}

class PermissionService {

    def isAuthorized(user: User, permission: Permission): Boolean = (user.id, permission) match {
        case (Some(1L), View) => true
        case (Some(1L), List) => true
        case (Some(2L), View) => true
        case (Some(2L), List) => false
        case _ => false
    }
}
