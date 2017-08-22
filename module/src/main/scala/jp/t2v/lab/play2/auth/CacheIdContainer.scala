package jp.t2v.lab.play2.auth

import java.security.SecureRandom
import javax.inject.Inject
import play.api.cache.SyncCacheApi
import scala.annotation.tailrec
import scala.util.Random
import scala.reflect.ClassTag
import scala.concurrent.duration._

trait IdType[A] {
  implicit val ct: ClassTag[A]
}

class CacheIdContainer[Id] @Inject() (cache: SyncCacheApi)(implicit ev: IdType[Id]) extends IdContainer[Id] {

  implicit val ct = ev.ct

  private[auth] val tokenSuffix = ":token"
  private[auth] val userIdSuffix = ":userId"
  private[auth] val random = new Random(new SecureRandom())

  def startNewSession(userId: Id, timeoutInSeconds: Int): AuthenticityToken = {
    removeByUserId(userId)
    val token = generate
    store(token, userId, timeoutInSeconds)
    token
  }

  @tailrec
  private[auth] final def generate: AuthenticityToken = {
    val table = "abcdefghijklmnopqrstuvwxyz1234567890_.~*'()"
    val token = Iterator.continually(random.nextInt(table.size)).map(table).take(64).mkString
    if (get(token).isDefined) generate else token
  }

  private[auth] def removeByUserId(userId: Id): Unit = {
    cache.get[String](userId.toString + userIdSuffix) foreach unsetToken
    unsetUserId(userId)
  }

  def remove(token: AuthenticityToken): Unit = {
    get(token) foreach unsetUserId
    unsetToken(token)
  }

  private[auth] def unsetToken(token: AuthenticityToken): Unit = {
    cache.remove(token + tokenSuffix)
  }
  private[auth] def unsetUserId(userId: Id): Unit = {
    cache.remove(userId.toString + userIdSuffix)
  }

  def get(token: AuthenticityToken): Option[Id] = cache.get(token + tokenSuffix)

  private[auth] def store(token: AuthenticityToken, userId: Id, timeoutInSeconds: Int): Unit = {
    cache.set(token + tokenSuffix, userId, timeoutInSeconds.seconds)
    cache.set(userId.toString + userIdSuffix, token, timeoutInSeconds.seconds)
  }

  def prolongTimeout(token: AuthenticityToken, timeoutInSeconds: Int): Unit = {
    get(token).foreach(store(token, _, timeoutInSeconds))
  }

}
