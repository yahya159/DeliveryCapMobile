package com.company.mysapbtpsdkproject.ui.odata.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.sap.cloud.mobile.kotlin.odata.StructureBase
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory

abstract class AbstractODataPageSource<T : StructureBase>(
    open val pageSize: Int,
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        val refreshKey = state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }

        LOGGER.debug("Get refresh key $refreshKey")
        return refreshKey
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        return try {
            val nextPageNo = params.key ?: 0
            val items = mutableListOf<T>()
            loadItems(pageSize, nextPageNo).collect {
                items.addAll(it)
            }
            LOGGER.debug("load with params {}", params)
            LoadResult.Page(
                data = items,
                prevKey = if (nextPageNo <= 0) null else nextPageNo - 1,
                nextKey = if (items.size < pageSize) null else {
                    val pages = items.size / pageSize
                    val remainder = items.size % pageSize
                    if (remainder > 0) null else {
                        nextPageNo + pages
                    }
                }
            )
        } catch (error: Exception) {
            LOGGER.error("Failed to load OData page", error)
            LoadResult.Error(error)
        }
    }

    protected abstract suspend fun loadItems(pageSize: Int, page: Int): Flow<List<T>>

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractODataPageSource::class.java)
    }

}
