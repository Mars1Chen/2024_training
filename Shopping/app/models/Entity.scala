package models

import scala.collection.mutable


class User(val userId: String, var cart: Cart)

class Cart(val productMap: mutable.Map[String, Int] = mutable.Map.empty)

case class Product(id: String, name: String, price: Double)