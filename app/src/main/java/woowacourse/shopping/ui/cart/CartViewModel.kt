package woowacourse.shopping.ui.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import woowacourse.shopping.common.Event
import woowacourse.shopping.data.cart.CartRepository
import woowacourse.shopping.data.cart.entity.CartItem
import woowacourse.shopping.data.product.ProductRepository
import woowacourse.shopping.model.Quantity
import woowacourse.shopping.ui.products.adapter.type.ProductUiModel

class CartViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) : ViewModel(), CartListener {
    private val _productUiModels = MutableLiveData<List<ProductUiModel>>(emptyList())
    val productUiModels: LiveData<List<ProductUiModel>> = _productUiModels

    private val totalCartCount = MutableLiveData<Int>(INITIALIZE_CART_SIZE)

    private val _page = MutableLiveData<Int>(INITIALIZE_PAGE)
    val page: LiveData<Int> get() = _page

    private val maxPage: LiveData<Int> = totalCartCount.map { (it - 1) / PAGE_SIZE }

    val hasPage: LiveData<Boolean> = totalCartCount.map { it > PAGE_SIZE }
    val hasPreviousPage: LiveData<Boolean> = _page.map { it > INITIALIZE_PAGE }
    val hasNextPage: LiveData<Boolean> = _page.map { it < (maxPage.value ?: INITIALIZE_PAGE) }
    val isEmptyCart: LiveData<Boolean> = _productUiModels.map { it.isEmpty() }

    private val _changedCartEvent = MutableLiveData<Event<Unit>>()
    val changedCartEvent: LiveData<Event<Unit>> get() = _changedCartEvent

    private val _pageLoadError = MutableLiveData<Event<Unit>>()
    val pageLoadError: LiveData<Event<Unit>> get() = _pageLoadError

    init {
        loadCart()
    }

    private fun loadCart(page: Int = _page.value ?: INITIALIZE_PAGE) {
        val cart = cartRepository.findRange(page, PAGE_SIZE)
        _productUiModels.value = cart.toProductUiModels()
        loadTotalCartCount()
    }

    private fun List<CartItem>.toProductUiModels(): List<ProductUiModel> {
        return map {
            val product = productRepository.find(it.productId)
            ProductUiModel.from(product, it.quantity)
        }
    }

    private fun loadTotalCartCount() {
        totalCartCount.value = cartRepository.totalCartItemCount()
    }

    override fun deleteCartItem(productId: Long) {
        _changedCartEvent.value = Event(Unit)
        cartRepository.deleteCartItem(productId)
        updateDeletedCart(productId)
    }

    private fun updateDeletedCart(productId: Long) {
        if (isEmptyLastPage()) {
            movePreviousPage()
            return
        }
        val products = _productUiModels.value ?: return
        val deletedProduct = products.find { productId == it.productId } ?: return
        _productUiModels.value = products - deletedProduct
        loadTotalCartCount()
    }

    private fun isEmptyLastPage(): Boolean {
        val page = _page.value ?: INITIALIZE_PAGE
        val totalCartCount = totalCartCount.value ?: INITIALIZE_CART_SIZE
        return page > 0 && totalCartCount % PAGE_SIZE == 1
    }

    fun moveNextPage() {
        _page.value = nextPage(_page.value ?: INITIALIZE_PAGE)
    }

    private fun nextPage(page: Int): Int {
        runCatching { loadCart(page + 1) }
            .onSuccess { return page + 1 }
            .onFailure { _pageLoadError.value = Event(Unit) }
        return page
    }

    fun movePreviousPage() {
        _page.value = previousPage(_page.value ?: INITIALIZE_PAGE)
    }

    private fun previousPage(page: Int): Int {
        runCatching { loadCart(page - 1) }
            .onSuccess { return page - 1 }
            .onFailure { _pageLoadError.value = Event(Unit) }
        return page
    }

    override fun increaseQuantity(productId: Long) {
        _changedCartEvent.value = Event(Unit)
        cartRepository.increaseQuantity(productId)

        val cartItem = cartRepository.findOrNull(productId) ?: return
        updateChangedCartQuantity(productId, cartItem.quantity)
    }

    override fun decreaseQuantity(productId: Long) {
        _changedCartEvent.value = Event(Unit)
        cartRepository.decreaseQuantity(productId)

        val cartItem = cartRepository.findOrNull(productId)
        if (cartItem == null) {
            updateDeletedCart(productId)
            return
        }

        updateChangedCartQuantity(productId, --cartItem.quantity)
    }

    private fun updateChangedCartQuantity(productId: Long, quantity: Quantity) {
        val position = findProductUiModelsPosition(productId) ?: return
        val productUiModels = _productUiModels.value?.toMutableList() ?: return
        productUiModels[position] = productUiModels[position].copy(quantity = quantity)
        _productUiModels.value = productUiModels
        loadTotalCartCount()
    }

    private fun findProductUiModelsPosition(productId: Long): Int? {
        return _productUiModels.value?.indexOfFirst { it.productId == productId }
    }

    companion object {
        private const val INITIALIZE_CART_SIZE = 0
        private const val INITIALIZE_PAGE = 0
        private const val PAGE_SIZE = 5
    }
}
