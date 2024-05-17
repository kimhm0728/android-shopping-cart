package woowacourse.shopping.feature.main.adapter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import woowacourse.shopping.data.product.ProductRepository
import woowacourse.shopping.feature.main.viewmodel.MainViewModel
import java.lang.IllegalArgumentException

class MainViewModelFactory(private val productRepository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(productRepository) as T
        }
        throw IllegalArgumentException()
    }
}