package commonClient.data.repository

import common.model.reseponse.PagingResponse
import common.model.reseponse.paging.NewPagingResponse
import commonClient.data.remote.endpoints.PostApi
import commonClient.domain.entity.post.Post
import commonClient.domain.mapper.toDomain
import commonClient.domain.repository.PostRepository
import commonClient.utils.map
import org.koin.core.annotation.Single

@Single(binds = [PostRepository::class])
class PostRepositoryImp(
    private val postApi: PostApi
) : PostRepository {

    override suspend fun postsById(postId: Long): Post {
        return postApi.getPost(postId).data!!.toDomain()
    }

    override suspend fun getPagedPost(categoryId: Long?, nextKey : Long?): PagingResponse<Post> {
        val response = postApi.getPagedPosts(categoryId, nextKey).data!!
        return response.map { it.toDomain() }
    }

    override suspend fun getPagedHotPost(offset: Int?, pageNumber: Int?, pageSize: Int?): NewPagingResponse<Post> {
        val response = postApi.getPagedHotPosts(offset, pageNumber, pageSize).data!!
        return response.map { it.toDomain() }
    }

    override suspend fun getPagedSearchPost(
        searchText: String,
        offset: Int?,
        pageNumber: Int?,
        pageSize: Int?
    ): NewPagingResponse<Post> {
        val response = postApi.getPagedSearchPosts(searchText, offset, pageNumber, pageSize).data!!
        return response.map { it.toDomain() }
    }
}