package woowacourse.shopping.data.cart

import woowacourse.shopping.model.CartItem
import woowacourse.shopping.model.Product
import woowacourse.shopping.model.Quantity
import java.lang.IllegalArgumentException
import kotlin.math.min

class DummyCartRepository private constructor() : CartRepository {
    private val cart: MutableList<CartItem> = mutableListOf()
    private var id: Long = 0L

    override fun increaseQuantity(product: Product) {
        val oldCartItem = cart.find { it.product.id == product.id }
        if (oldCartItem == null) {
            cart.add(CartItem(id++, product, Quantity(1)))
            return
        }
        var quantity = oldCartItem.quantity
        cart.remove(oldCartItem)
        cart.add(oldCartItem.copy(quantity = ++quantity))
    }

    override fun decreaseQuantity(product: Product) {
        val oldCartItem = cart.find { it.product.id == product.id }
        oldCartItem ?: throw IllegalArgumentException(CANNOT_DELETE_MESSAGE)
        cart.remove(oldCartItem)
        if (oldCartItem.quantity.isMin()) {
            return
        }
        var quantity = oldCartItem.quantity
        cart.add(oldCartItem.copy(quantity = --quantity))
    }

    override fun deleteCartItem(cartItem: CartItem) {
        cart.remove(cartItem)
    }

    override fun findRange(
        page: Int,
        pageSize: Int,
    ): List<CartItem> {
        val fromIndex = page * pageSize
        val toIndex = min(fromIndex + pageSize, cart.size)
        return cart.subList(fromIndex, toIndex)
    }

    override fun totalProductCount(): Int = cart.size

    override fun totalQuantityCount(): Int {
        return cart.fold(0) { total, cartItem ->
            total + cartItem.quantity.count
        }
    }

    companion object {
        private const val CANNOT_DELETE_MESSAGE = "삭제할 수 없습니다."

        @Volatile
        private var instance: DummyCartRepository? = null

        fun getInstance(): DummyCartRepository {
            return instance ?: synchronized(this) {
                DummyCartRepository()
            }
        }
    }
}
