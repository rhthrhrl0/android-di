package woowacourse.util

import android.content.Context
import woowacourse.shopping.data.CartRepository
import woowacourse.shopping.data.ProductRepository
import woowacourse.shopping.di.module.ActivityModule
import woowacourse.shopping.di.module.ApplicationModule
import woowacourse.shopping.model.Product

class FakeApplicationModule(context: Context) : ApplicationModule(context) {
    private val cartRepository = getFakeCartRepository()
    fun getCartRepository(): CartRepository = cartRepository
}

fun getFakeApplicationModule(context: Context): ApplicationModule = FakeApplicationModule(context)

fun getFakeActivityModule(
    context: Context,
    fakeApplicationModule: ApplicationModule,
): ActivityModule =
    object : ActivityModule(context, fakeApplicationModule) {
        fun getProductRepository(): ProductRepository {
            return getFakeProductRepository()
        }
    }

fun getFakeCartRepository(): CartRepository = object : CartRepository {
    private val carts = mutableListOf<Product>()

    override fun addCartProduct(product: Product) {
        carts.add(product)
    }

    override fun getAllCartProducts(): List<Product> {
        return carts.toList()
    }

    override fun deleteCartProduct(id: Int) {
        carts.removeAt(id)
    }
}

fun getFakeProductRepository(): ProductRepository = object : ProductRepository {
    private val products: List<Product> = getProducts()

    override fun getAllProducts(): List<Product> {
        return products
    }
}

fun getProducts(names: List<String> = listOf("사과", "포도")): List<Product> {
    return mutableListOf<Product>().apply {
        names.forEach {
            add(getProduct(it))
        }
    }
}

fun getProduct(
    name: String,
    price: Int = 10_000,
    imageUrl: String = "https://cdn-mart.baemin.com/sellergoods/api/main/711c39ee-ff8e-4983-aa01-f669e82bddae.jpg?h=700&w=700",
): Product {
    return Product(name, price, imageUrl)
}
