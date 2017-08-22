package modules

import controllers._
import jp.t2v.lab.play2.auth._
import play.api.{ Configuration, Environment }
import play.api.inject._
import com.google.inject.TypeLiteral
import com.google.inject.AbstractModule

class AuthModule extends AbstractModule {

  def configure(): Unit = {
    bind(new TypeLiteral[AuthConfig[MyEnv]]() {}).to(classOf[MyAuthConfig])
    bind(classOf[TokenAccessor]).to(classOf[CookieTokenAccessor])
    bind(new TypeLiteral[IdContainer[Long]] {}).to(new TypeLiteral[CacheIdContainer[Long]] {})
    bind(new TypeLiteral[IdType[Long]] {}).to(classOf[LongId])
  }

}
