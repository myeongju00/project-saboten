package common.model.reseponse.post.read

import common.model.reseponse.category.CategoryResponse
import common.model.reseponse.post.VoteResponse
import common.model.reseponse.user.UserResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//내가 쓴 게시글 DTO
@Serializable
data class PostReadResponse (
    @SerialName("id") val id: Long,
    @SerialName("text") val text: String,
    @SerialName("author") val author: UserResponse,
    @SerialName("votes") val votes: List<VoteResponse>,
    @SerialName("categories") val categories: List<CategoryResponse>,
    @SerialName("selectedVote") val selectedVote: Long? = null,
    @SerialName("isScraped") val isScraped: Boolean,
    @SerialName("isLiked") val isLiked: Boolean,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String?
)