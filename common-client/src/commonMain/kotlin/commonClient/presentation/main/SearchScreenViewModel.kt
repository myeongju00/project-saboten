package commonClient.presentation.main

import com.kuuurt.paging.multiplatform.PagingData
import com.kuuurt.paging.multiplatform.PagingResult
import com.kuuurt.paging.multiplatform.helpers.cachedIn
import common.model.request.post.VoteSelectRequest
import commonClient.domain.entity.PagingRequest
import commonClient.domain.entity.post.Post
import commonClient.domain.usecase.post.AddRecentSearchTextsUseCase
import commonClient.domain.usecase.post.ClearRecentSearchTextsUseCase
import commonClient.domain.usecase.post.GetRecentSearchTextsUseCase
import commonClient.domain.usecase.post.GetSearchedPostCountUseCase
import commonClient.domain.usecase.post.RemoveRecentSearchTextsUseCase
import commonClient.domain.usecase.post.RequestLikePostUseCase
import commonClient.domain.usecase.post.RequestScrapPostUseCase
import commonClient.domain.usecase.post.RequestVotePostUseCase
import commonClient.domain.usecase.post.paged.GetPagedSearchPostsUseCase
import commonClient.presentation.PlatformViewModel
import commonClient.presentation.container
import commonClient.utils.createPager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce

data class SearchScreenState(
    val searchHistories: List<String> = emptyList(),
    val lastSearchedQuery: String? = null,
    val totalCount: Long? = null,
    val items: Flow<PagingData<Post>> = flowOf(),
)

interface SearchScreenEffect {

}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchScreenViewModel(
    private val getPagedSearchPostsUseCase: GetPagedSearchPostsUseCase,
    private val addRecentSearchTextsUseCase: AddRecentSearchTextsUseCase,
    private val removeRecentSearchTextsUseCase: RemoveRecentSearchTextsUseCase,
    private val clearRecentSearchTextsUseCase: ClearRecentSearchTextsUseCase,
    private val getRecentSearchTextsUseCase: GetRecentSearchTextsUseCase,
    private val requestScrapPostUseCase: RequestScrapPostUseCase,
    private val requestVotePostUseCase: RequestVotePostUseCase,
    private val requestLikePostUseCase: RequestLikePostUseCase,
    private val getSearchedPostCountUseCase: GetSearchedPostCountUseCase
) : PlatformViewModel<SearchScreenState, SearchScreenEffect>() {

    override val container: Container<SearchScreenState, SearchScreenEffect> =
        container(SearchScreenState())

    init {
        intent {
            getRecentSearchTextsUseCase()
                .onEach {
                    reduce { state.copy(searchHistories = it) }
                }
                .launchIn(platformViewModelScope)
        }
    }

    fun removeSearchHistory(history: String) = intent {
        removeRecentSearchTextsUseCase(history)
    }

    fun clearSearchHistory() = intent {
        clearRecentSearchTextsUseCase()
    }

    fun search(query: String) = intent {
        addRecentSearchTextsUseCase(query)

        intent {
            val searchedPostCount = getSearchedPostCountUseCase(query)
            reduce {
                state.copy(lastSearchedQuery = null, totalCount = searchedPostCount)
            }
        }

        reduce {

            val searchedPager = createPager<Long, Post>(20, -1) { key, _ ->
                val pagingResult = getPagedSearchPostsUseCase(query, PagingRequest(page = key))

                intent {
                    reduce {
                        state.copy(
                            lastSearchedQuery = query,
                        )
                    }
                }

                PagingResult(
                    pagingResult.data,
                    currentKey = key ?: -1,
                    prevKey = { null },
                    nextKey = { pagingResult.nextKey }
                )
            }

            state.copy(
                items = searchedPager.pagingData.cachedIn(platformViewModelScope)
            )
        }
    }

    private val _updatedPostCache = MutableStateFlow(mutableListOf<Post>())
    val updatedPostCache: StateFlow<List<Post>> = _updatedPostCache

    private fun updatePost(post: Post) {
        intent {
            val updatedPostCache = _updatedPostCache.value
            updatedPostCache.removeAll { it.id == post.id }
            _updatedPostCache.emit((updatedPostCache + post).toMutableList())
        }
    }


    fun requestVote(postId: Long, voteId: Long) {
        intent {
            val post = requestVotePostUseCase(postId, VoteSelectRequest(voteId))
            updatePost(post)
        }
    }

    fun requestLike(postId: Long) {
        intent {
            val post = requestLikePostUseCase(postId)
            updatePost(post)
        }
    }

    fun requestScrap(postId: Long) {
        intent {
            val post = requestScrapPostUseCase(postId)
            updatePost(post)
        }
    }

}