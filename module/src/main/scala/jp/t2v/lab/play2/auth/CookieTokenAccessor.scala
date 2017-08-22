package jp.t2v.lab.play2.auth

import javax.inject._
import play.api.mvc.{DiscardingCookie, Cookie, Result, RequestHeader}
import play.api.libs.crypto.CookieSigner

class CookieTokenAccessor @Inject() (val signer: CookieSigner) extends TokenAccessor {

  // Inject the Play Configuration and pull these from there, instead
  protected val cookieName: String = "PLAY2AUTH_SESS_ID"
  protected val cookieSecureOption: Boolean = false
  protected val cookieHttpOnlyOption: Boolean = true
  protected val cookieDomainOption: Option[String] = None
  protected val cookiePathOption: String = "/"
  protected val cookieMaxAge: Option[Int] = None

  def put(token: AuthenticityToken)(result: Result)(implicit request: RequestHeader): Result = {
    val c = Cookie(cookieName, sign(token), cookieMaxAge, cookiePathOption, cookieDomainOption, cookieSecureOption, cookieHttpOnlyOption)
    result.withCookies(c)
  }

  def extract(request: RequestHeader): Option[AuthenticityToken] = {
    request.cookies.get(cookieName).flatMap(c => verifyHmac(c.value))
  }

  def delete(result: Result)(implicit request: RequestHeader): Result = {
    result.discardingCookies(DiscardingCookie(cookieName))
  }

}
