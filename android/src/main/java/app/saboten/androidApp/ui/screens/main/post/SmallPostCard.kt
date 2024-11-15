package app.saboten.androidApp.ui.screens.main.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.saboten.androidApp.extensions.asDurationStringFromNow
import app.saboten.androidApp.ui.providers.LocalMeInfo
import app.saboten.androidApp.ui.screens.LocalOpenLoginDialogEffect
import app.saboten.androidUi.image.NetworkImage
import app.saboten.androidUi.styles.SabotenColors
import app.saboten.androidUi.utils.sabotenShadow
import commonClient.domain.entity.post.Post

@Composable
fun SmallPostCard(
    post: Post,
    onClicked: () -> Unit,
    onScrapClicked: () -> Unit = {},
    onLikeClicked: () -> Unit = {},
    onCommentClicked: () -> Unit = {}
) {

    val meState = LocalMeInfo.current
    val openLoginDialog = LocalOpenLoginDialogEffect.current

    Box(
        modifier = Modifier
            .width(320.dp)
            .height(168.dp)
            .sabotenShadow()
            .background(
                color = MaterialTheme.colors.surface,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClicked),
    ) {
        Box(
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 15.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        NetworkImage(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape),
                            url = post.author.profilePhotoUrl
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Column() {
                            Text(
                                post.author.nickname,
                                fontSize = 12.sp
                            )
                            Text(
                                post.createdAt.asDurationStringFromNow(),
                                fontSize = 10.sp,
                                color = MaterialTheme.colors.onSurface.copy(0.5f)
                            )
                        }
                    }

                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                if(meState.needLogin) openLoginDialog()
                                else onScrapClicked()
                            },
                        imageVector = Icons.Rounded.Bookmark,
                        tint =
                        if (post.isScraped == true) SabotenColors.green500 else MaterialTheme.colors.onSurface.copy(0.2f),
                        contentDescription = "북마크"
                    )
                }
                Spacer(modifier = Modifier.padding(vertical = 9.dp))

                Box(
                    modifier = Modifier
                        .height(45.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = post.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row() {
                    post.categories.take(2).forEach {
                        GroupItem(it.name)
                        Spacer(modifier = Modifier.padding(horizontal = 3.dp))
                    }
                }

                Row() {
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(24.dp)
                            .clickable {
                                if(meState.needLogin) openLoginDialog()
                                else onLikeClicked()
                            },
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "하트",
                        tint =
                        if (post.isLiked == true) SabotenColors.green500 else MaterialTheme.colors.onSurface.copy(0.2f)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Icon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(24.dp)
                            .clickable {
                                onCommentClicked()
                            },
                        imageVector = Icons.Rounded.Forum,
                        contentDescription = "댓글",
                        tint = MaterialTheme.colors.onSurface.copy(0.2f)
                    )
                }
            }
        }
    }
}
