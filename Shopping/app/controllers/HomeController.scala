package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import models.Operate
import play.api.libs.json.Json
import models.{User, Cart, Product}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)extends BaseController {


    //    def helloWorld(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    //        Ok("Hello, World!")
    //    }
    //
    //    def helloWorldAsync(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    //        val futureResult: Future[String] = Future {
    //            // Simulate a long-running task
    //            Thread.sleep(3000)
    //            "Hello, World!"
    //        }
    //        futureResult.map { result =>
    //            Ok(result)
    //        }
    //    }
    //
    //    def hello(name: String): Action[AnyContent] = Action {
    //        Ok("Hello " + name + "!")
    //    }

    def homePage(): Action[AnyContent] = Action {
        Ok("Welcome to out shopping mart!")
    }

    def initUser(userId: String): Action[AnyContent] = Action {
        val cart = Operate.initUser(userId)
        if (cart.productMap.isEmpty){
            Ok(s"用户 ${userId} 成功完成注册！")
        }else{
            Ok(s"用户 ${userId} 已经注册过了。")
        }
    }

    def getUserList: Action[AnyContent] = Action {
        val userMap = Operate.getUserMap
        val userList: List[String] = userMap.keys.toList
        Ok(s"当前商城用户如下: ${userList}")
    }

    def addItem(userId: String, productId: String, quantity: Int): Action[AnyContent] = Action {
        Operate.addItem(userId, productId, quantity)
        Ok(Json.obj("status" -> "success", "message" -> s"向 ${userId} 的购物车中添加了 ${quantity} 个 ${productId} 商品"))
    }

    def checkCart(userId: String): Action[AnyContent] = Action {
        val cartMap = Operate.checkCart(userId)
        val items = cartMap.map { case (product, quantity) =>
            Json.obj(
                "productId" -> product.id,
                "productName" -> product.name,
                "price" -> product.price,
                "quantity" -> quantity
            )
        }
        Ok(Json.obj("status" -> "success", "cartItems" -> items))
    }

    def clearCart(userId: String): Action[AnyContent] = Action {
        val totalPrice = Operate.clearCart(userId)
        Ok(s"您的购物车已成功清空！商品总价为 ${totalPrice} 元")
    }

}
