package models

import scala.collection.{immutable, mutable}

object Operate {
    val userMap = mutable.Map[String, User]()
    private val products: immutable.Map[String, Product] = Map(
        "p001" -> Product("p001", "Scala Book", 39.99),
        "p002" -> Product("p002", "Akka Concurrency", 49.99),
        "p003" -> Product("p003", "Play Framework Guide", 59.99),
        "p004" -> Product("p004", "Scala Test Kit", 29.99),
        "p005" -> Product("p005", "Spark Data Processing", 69.99)
    )

    def getUserMap: Map[String, User] = {
        userMap.toMap
    }

    def initUser(userId: String): Cart = {
        userMap.get(userId) match {
            case Some(user) => user.cart
            case None =>
                val cart = new Cart()
                userMap += (userId -> new User(userId, cart))
                cart
        }
    }

    private def getCart(userId: String): Cart = {
        val cart = userMap.get(userId) match {
            case Some(value) => value.cart
            case None => initUser(userId)
        }
        cart
    }

    // 添加商品到购物车
    def addItem(userId: String, productId: String, quantity: Int): Unit = {
        require(quantity < 100, "单个商品的数量上限为99个！")
        val cart = getCart(userId)
        cart.productMap += (productId -> (cart.productMap.getOrElse(productId, 0) + quantity))
    }

    // 查看购物车信息
    def checkCart(userId: String): Map[Product, Int] = {
        val cart = getCart(userId)
        val cartMap = mutable.Map[Product, Int]()

        for((productId, quantity) <- cart.productMap) {
            val productName = products.get(productId).map(_.name).getOrElse("该商品ID有误")
            val price = products.get(productId).map(_.price).getOrElse(0.0)
            cartMap += (Product(productId, productName, price) -> quantity)
        }
        cartMap.toMap
    }

    // 清空购物车
    def clearCart(userId: String): Double = {
        val cart = getCart(userId)
        var totalPrice: Double = 0.0
        for((productId, quantity) <- cart.productMap) {
            val price = products.get(productId).map(_.price).getOrElse(0.0)
            totalPrice += price * quantity
        }
        cart.productMap.clear()
        totalPrice
    }

}
