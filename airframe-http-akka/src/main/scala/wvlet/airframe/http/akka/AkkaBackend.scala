package wvlet.airframe.http.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCode}
import wvlet.airframe.http.{HttpBackend, HttpRequestAdapter, HttpStatus}

import scala.concurrent.{ExecutionContext, Future}

class AkkaBackend(implicit actorSystem: ActorSystem) extends HttpBackend[HttpRequest, HttpResponse, Future] {
  override protected implicit val httpRequestAdapter: HttpRequestAdapter[HttpRequest] = new AkkaHttpRequestAdapter()

  override def newResponse(status: HttpStatus, content: String): HttpResponse =
    HttpResponse(StatusCode.int2StatusCode(status.code)).withEntity(content)

  override def toFuture[A](a: A): Future[A] = Future.successful(a)

  override def toFuture[A](a: Future[A], e: ExecutionContext): Future[A] = a

  override def toScalaFuture[A](a: Future[A]): Future[A] = a

  override def wrapException(e: Throwable): Future[HttpResponse] = Future.failed(e)

  override def isFutureType(cl: Class[_]): Boolean = {
    classOf[Future[_]].isAssignableFrom(cl)
  }
  override def isRawResponseType(cl: Class[_]): Boolean = {
    classOf[HttpResponse].isAssignableFrom(cl)
  }

  override def mapF[A, B](f: Future[A], body: A => B): Future[B] = {
    import actorSystem.dispatcher
    f.map(body)
  }

  override def withThreadLocalStore(request: => Future[HttpResponse]): Future[HttpResponse] = ???

  override def setThreadLocal[A](key: String, value: A): Unit = ???

  override def getThreadLocal[A](key: String): Option[A] = ???
}
