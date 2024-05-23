package woowacourse.shopping.ui.products

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import woowacourse.shopping.data.cart.CartRepository
import woowacourse.shopping.data.product.ProductRepository
import woowacourse.shopping.data.recent.DummyRecentProductRepository
import woowacourse.shopping.databinding.ActivityProductsBinding
import woowacourse.shopping.ui.cart.CartActivity
import woowacourse.shopping.ui.detail.ProductDetailActivity
import woowacourse.shopping.ui.products.adapter.ProductsAdapter
import woowacourse.shopping.ui.products.adapter.ProductsSpanSizeLookUp

class ProductsActivity : AppCompatActivity() {
    private val binding: ActivityProductsBinding by lazy {
        ActivityProductsBinding.inflate(layoutInflater)
    }
    private val viewModel by viewModels<ProductsViewModel> {
        ProductsViewModelFactory(
            ProductRepository.getInstance(),
            DummyRecentProductRepository,
            CartRepository.getInstance(),
        )
    }
    private lateinit var adapter: ProductsAdapter

    private val productDetailActivityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val changedProductId =
                    result.data?.getLongExtra(
                        CHANGED_PRODUCT_ID_KEY,
                        CHANGED_PRODUCT_ID_DEFAULT_VALUE
                    )
                        ?: return@registerForActivityResult
                viewModel.loadProductUiModel(changedProductId)
            }
        }

    private val cartActivityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val isChangedCart =
                    result.data?.getBooleanExtra(IS_CHANGED_CART_KEY, IS_CHANGED_CART_BOOLEAN)
                        ?: return@registerForActivityResult
                if (!isChangedCart) return@registerForActivityResult
                viewModel.updateProducts()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        initializeView()
    }

    private fun initializeView() {
        initializeProductAdapter()
        initializeToolbar()
        initializePage()
    }

    private fun initializeProductAdapter() {
        adapter =
            ProductsAdapter(
                onClickProductItem = { navigateToProductDetailView(it) },
                onIncreaseProductQuantity = { viewModel.increaseQuantity(it) },
                onDecreaseProductQuantity = { viewModel.decreaseQuantity(it) },
            )
        binding.rvProducts.itemAnimator = null
        binding.rvProducts.layoutManager =
            GridLayoutManager(this, PRODUCT_LIST_SPAN_SIZE).apply {
                spanSizeLookup = ProductsSpanSizeLookUp(adapter)
            }
        binding.rvProducts.adapter = adapter
        viewModel.productUiModels.observe(this) {
            adapter.insertProducts(it)
        }
        viewModel.changedProductQuantity.observe(this) {
            adapter.replaceProduct(it)
        }
        viewModel.recentProducts.observe(this) {
            adapter.addRecentProducts(it ?: return@observe)
        }
    }

    private fun navigateToProductDetailView(productId: Long) {
        val intent = ProductDetailActivity.newIntent(this, productId)
        productDetailActivityResultLauncher.launch(intent)
    }

    private fun initializeToolbar() {
        binding.ivProductsCart.setOnClickListener { navigateToCartView() }
        binding.tvProductsCartCount.setOnClickListener { navigateToCartView() }
    }

    private fun navigateToCartView() {
        val intent = Intent(this, CartActivity::class.java)
        cartActivityResultLauncher.launch(intent)
    }

    private fun initializePage() {
        val onScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    super.onScrolled(recyclerView, dx, dy)
                    val lastPosition =
                        (recyclerView.layoutManager as GridLayoutManager).findLastCompletelyVisibleItemPosition()
                    viewModel.changeSeeMoreVisibility(lastPosition)
                }
            }
        binding.rvProducts.addOnScrollListener(onScrollListener)
    }

    companion object {
        private const val PRODUCT_LIST_SPAN_SIZE = 2

        const val CHANGED_PRODUCT_ID_KEY = "changed_product_id_key"
        private const val CHANGED_PRODUCT_ID_DEFAULT_VALUE = -1L

        const val IS_CHANGED_CART_KEY = "is_changed_cart_key"
        private const val IS_CHANGED_CART_BOOLEAN = false
    }
}
