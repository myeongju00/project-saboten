package common.model.request.post

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* POST /post/{post_id}/like */
@Serializable
data class PostLikeRequest(
    @SerialName("isLiked") val isLiked: Boolean,
)