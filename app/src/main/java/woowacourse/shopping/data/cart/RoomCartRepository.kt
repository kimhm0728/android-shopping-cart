package woowacourse.shopping.data.cart

import woowacourse.shopping.data.cart.dao.CartDao
import woowacourse.shopping.data.cart.entity.CartItem
import woowacourse.shopping.model.Quantity
import kotlin.IllegalArgumentException
import kotlin.concurrent.thread

class RoomCartRepository(private val cartDao: CartDao) : CartRepository {
    override fun increaseQuantity(productId: Long) {
        thread {
            runCatching {
                cartDao.findCartItem(productId)
            }.onSuccess {
                var oldQuantity = it.quantity
                cartDao.changeQuantity(productId, ++oldQuantity)
            }.onFailure {
                val cartItem = CartItem(productId = productId, quantity = Quantity(1))
                cartDao.insertCartItem(cartItem)
            }
        }
    }

    override fun decreaseQuantity(productId: Long) {
        thread {
            runCatching {
                cartDao.findCartItem(productId)
            }.onSuccess {
                var oldQuantity = it.quantity
                if (oldQuantity.count == 1) {
                    cartDao.deleteCartItem(productId)
                }
                cartDao.changeQuantity(productId, --oldQuantity)
            }.onFailure {
                throw IllegalArgumentException(CANNOT_DELETE_MESSAGE)
            }
        }
    }

    override fun changeQuantity(
        productId: Long,
        quantity: Quantity,
    ) {
        thread {
            runCatching {
                cartDao.findCartItem(productId)
            }.onSuccess {
                cartDao.changeQuantity(productId, quantity)
            }.onFailure {
                val cartItem = CartItem(productId = productId, quantity = quantity)
                cartDao.insertCartItem(cartItem)
            }
        }
    }

    override fun deleteCartItem(productId: Long) {
        thread {
            runCatching {
                cartDao.deleteCartItem(productId)
            }.onFailure {
                throw IllegalArgumentException(CANNOT_DELETE_MESSAGE)
            }
        }
    }

    override fun find(productId: Long): CartItem {
        var cartItem: CartItem? = null
        thread {
            cartItem =
                runCatching { cartDao.findCartItem(productId) }
                    .getOrNull()
        }.join()
        return cartItem ?: throw IllegalArgumentException(CANNOT_FIND_MESSAGE)
    }

    override fun findRange(page: Int, pageSize: Int): List<CartItem> {
        var cartItems: List<CartItem> = emptyList()
        thread {
            cartItems = cartDao.findCartItemRange(page, pageSize)
        }.join()
        return cartItems
    }

    override fun totalCartItemCount(): Int {
        var totalCartItemCount = 0
        thread {
            totalCartItemCount = cartDao.totalCartItemCount()
        }.join()
        return totalCartItemCount
    }

    companion object {
        private const val CANNOT_DELETE_MESSAGE = "삭제할 수 없습니다."
        private const val CANNOT_FIND_MESSAGE = "해당하는 장바구니 상품이 존재하지 않습니다."
    }
}
