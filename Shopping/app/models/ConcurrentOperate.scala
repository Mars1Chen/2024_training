package models

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object ConcurrentOperate {
    sealed trait Command
    case class InitUser(userId: String, replyTo: ActorRef[String]) extends Command
    case class AddItem(userId: String, productId: String, quantity: Int, replyTo: ActorRef[String]) extends Command
    case class CheckCart(userId: String, replyTo: ActorRef[Map[Product, Int]]) extends Command
    case class ClearCart(userId: String, replyTo: ActorRef[Double]) extends Command

    def apply(): Behavior[Command] = {
        Behaviors.receive { (context, message) =>
            message match {
                case InitUser(userId, replyTo) =>
                    val cart = Operate.initUser(userId)
                    if (cart.productMap.isEmpty) {
                        replyTo ! s"用户 ${userId} 成功完成注册！"
                    } else {
                        replyTo ! s"用户 ${userId} 已经注册过了。"
                    }
                    Behaviors.same

                case AddItem(userId, productId, quantity, replyTo) =>
                    Operate.addItem(userId, productId, quantity)
                    replyTo ! s"向 ${userId} 的购物车中添加了 ${quantity} 个 ${productId} 商品"
                    Behaviors.same

                case CheckCart(userId, replyTo) =>
                    val cartMap = Operate.checkCart(userId)
                    replyTo ! cartMap
                    Behaviors.same

                case ClearCart(userId, replyTo) =>
                    val totalPrice = Operate.clearCart(userId)
                    replyTo ! totalPrice
                    Behaviors.same
            }
        }
    }
}
