package controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CartSpec extends AnyWordSpec with Matchers {

    import models.{User, Cart, Product}
    import models.Operate._


    "addItem" should {
        "add items to user's cart" in {
            userMap.clear()
            // 初始化用户
            val userId = "user1"
            val user = initUser(userId)

            // 调用 addItem 方法
            addItem(userId, "p001", 3)
            addItem(userId, "p002", 2)

            // 验证购物车内容
            userMap(userId).cart.productMap should contain allOf("p001" -> 3, "p002" -> 2)
        }
        "handle quantity limit" in {
            userMap.clear()
            // 初始化用户
            val userId = "user2"
            initUser(userId)

            // 调用 addItem 方法，添加超过限制的数量
            val exception = intercept[IllegalArgumentException] {
                addItem(userId, "p003", 100)
            }

            // 验证异常信息
            exception.getMessage should be("requirement failed: 单个商品的数量上限为99个！")

            // 验证购物车内容，应该没有添加超过限制的商品
            userMap(userId).cart.productMap should not contain key("p003")
        }
    }


    "checkCart" should {
        "print products in user's cart correctly" in {
            userMap.clear()
            val userId = "user3"
            initUser(userId)
            addItem(userId, "p001", 13)
            addItem(userId, "p002", 12)

            val cartMap = checkCart(userId)
            cartMap should contain(Product("p001", "Scala Book", 39.99) -> 13)
            cartMap should contain(Product("p002", "Akka Concurrency", 49.99) -> 12)
        }
        "print empty cart message" in {
            userMap.clear()
            val userId = "user4"
            initUser(userId)

            val cartMap = checkCart(userId)
            cartMap should be(empty)
        }
    }

    "clearCart" should {
        "calculate the total price and clear the cart" in {
            val userId = "user5"
            initUser(userId)
            addItem(userId, "p001", 5)
            addItem(userId, "p004", 2)
            addItem(userId, "p005", 3)
            val totalPrice = clearCart(userId)
            totalPrice should be(5 * 39.99 + 2 * 29.99 + 3 * 69.99)
            userMap(userId).cart.productMap should be(empty)
        }
    }
}
