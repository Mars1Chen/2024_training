package controllers

import akka.actor.typed.{ActorRef, ActorSystem, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import models.ConcurrentOperate

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import models.ConcurrentOperate._
import play.api.libs.json.Json

@Singleton
class ShoppingCartController @Inject()(cc: ControllerComponents, system: ActorSystem[Nothing])(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

    implicit private val timeout: Timeout = Timeout(3.seconds)
    implicit val scheduler: Scheduler = system.scheduler
    private val shoppingCartActor: ActorRef[ConcurrentOperate.Command] = system.systemActorOf(ConcurrentOperate(), "shoppingCartActor")

    // 初始化用户
    def initUser(userId: String): Action[AnyContent] = Action.async {
        val replyTo: ActorRef[String] = system.ignoreRef[String]
        val initUserFuture: Future[String] = shoppingCartActor.ask(InitUser(userId, _))
        initUserFuture.map { message =>
            Ok(message)
        }
    }

    // 添加商品到购物车
    def addItem(userId: String, productId: String, quantity: Int): Action[AnyContent] = Action.async {
        val replyTo: ActorRef[String] = system.ignoreRef[String]
        val addItemFuture: Future[String] = shoppingCartActor.ask(AddItem(userId, productId, quantity, _))
        addItemFuture.map { message =>
            Ok(Json.obj("status" -> "success", "message" -> message))
        }
    }

    // 查看购物车信息
    def checkCart(userId: String): Action[AnyContent] = Action.async {
        val replyTo: ActorRef[Map[Product, Int]] = system.ignoreRef[Map[Product, Int]]
        val checkCartFuture: Future[Map[Product, Int]] = shoppingCartActor.ask(CheckCart(userId, _))
        checkCartFuture.map { cart =>
            val items = cart.map { case (product, quantity) =>
                Json.obj(
                    "productId" -> product.id,
                    "productName" -> product.name,
                    "price" -> product.price,
                    "quantity" -> quantity
                )
            }
            Ok(Json.obj("status" -> "success", "cartItems" -> items))
        }
    }

    // 清空购物车
    def clearCart(userId: String): Action[AnyContent] = Action.async {
        val replyTo: ActorRef[Double] = system.ignoreRef[Double]
        val clearCartFuture: Future[Double] = shoppingCartActor.ask(ClearCart(userId, _))
        clearCartFuture.map { totalPrice =>
            Ok(s"您的购物车已成功清空！商品总价为 ${totalPrice} 元")
        }
    }
}
