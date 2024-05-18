package woowacourse.shopping.feature.cart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import woowacourse.shopping.data.cart.CartRepository
import woowacourse.shopping.model.CartItem
import woowacourse.shopping.model.Product

class CartViewModel(private val cartRepository: CartRepository) : ViewModel() {
    private val _cart = MutableLiveData<List<CartItem>>()
    val cart: LiveData<List<CartItem>> get() = _cart

    private val _cartSize = MutableLiveData<Int>()
    val cartSize: LiveData<Int> get() = _cartSize

    fun delete(product: Product) {
        cartRepository.deleteCartItem(product)
    }

    fun loadCart(
        page: Int,
        pageSize: Int,
    ) {
        _cart.value = cartRepository.findRange(page, pageSize)
    }

    fun loadCount() {
        _cartSize.value = cartRepository.count()
    }
}
