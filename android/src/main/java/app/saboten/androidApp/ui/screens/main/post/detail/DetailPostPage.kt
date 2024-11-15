package app.saboten.androidApp.ui.screens.main.post.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.saboten.androidApp.ui.providers.LocalMeInfo
import app.saboten.androidApp.ui.screens.main.post.LargePostCard
import app.saboten.androidUi.bars.BasicTopBar
import app.saboten.androidUi.bars.ToolbarBackButton
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import commonClient.presentation.post.DetailPostScreenViewModel
import org.koin.androidx.compose.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import app.saboten.androidApp.extensions.asDurationStringFromNow
import app.saboten.androidApp.ui.destinations.PostSettingDialogDestination
import app.saboten.androidApp.ui.dialog.PostSettingDialogResult
import app.saboten.androidApp.ui.providers.MeInfo
import app.saboten.androidUi.buttons.FilledButton
import app.saboten.androidUi.dialogs.BasicDialog
import app.saboten.androidUi.dialogs.DialogContentGravity
import app.saboten.androidUi.image.NetworkImage
import app.saboten.androidUi.styles.SabotenColors
import app.saboten.androidUi.styles.surfaceOver
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import commonClient.presentation.post.DetailPostScreenEffect
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
@Destination
fun DetailPostScreen(
    postId: Long,
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<PostSettingDialogDestination, PostSettingDialogResult>
) {
    val viewModel = koinViewModel<DetailPostScreenViewModel>()

    val meState = LocalMeInfo.current

    val context = LocalContext.current

    LaunchedEffect(meState.needLogin) {
        viewModel.loadPost(postId)
    }

    DetailPostPageContent(viewModel, meState,
    onBackPressed = {
        navigator.popBackStack()
    },
    onKebabButtonClicked = {
        navigator.navigate(PostSettingDialogDestination)
    })

    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {

            }

            is NavResult.Value<PostSettingDialogResult> -> {
                when (result.value) {
                    is PostSettingDialogResult.Delete -> {
                        viewModel.setShowDeletePostConfirmDialogState(true)
                    }
                    is PostSettingDialogResult.Modify -> {
                        Toast.makeText(context, "준비 중인 기능입니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPostPageContent(
    viewModel: DetailPostScreenViewModel,
    meState: MeInfo,
    onBackPressed: () -> Unit,
    onKebabButtonClicked: () -> Unit,
) {

    val context = LocalContext.current

    val state by viewModel.collectAsState()

    var query by remember { mutableStateOf("") }
    var isPostingComment by remember { mutableStateOf(false) }
    var isDeletingPost by remember { mutableStateOf(false) }

    val post = remember(state.post) { state.post.getDataOrNull() }

    val comments = state.comments.collectAsLazyPagingItems()

    viewModel.collectSideEffect {
        when (it) {
            is DetailPostScreenEffect.CommentPosted -> {
                isPostingComment = false
                query = ""
                viewModel.refreshComment()
            }

            is DetailPostScreenEffect.CommentPosting -> {
                isPostingComment = true
            }

            is DetailPostScreenEffect.CommentPostFailed -> {
                isPostingComment = false
            }

            is DetailPostScreenEffect.PostDeleting -> {
                isDeletingPost = true
            }

            is DetailPostScreenEffect.PostDeleted -> {
                isDeletingPost = false
                Toast.makeText(context, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                onBackPressed()
            }

            is DetailPostScreenEffect.PostDeleteFailed -> {
                isDeletingPost = false
            }

        }
    }

    Scaffold(
        topBar = {
            BasicTopBar(title = { /*TODO*/ }, navigationIcon = {
                ToolbarBackButton(onBackPressed)
            }, actions = {
                if (post?.author?.id == meState.userInfo.getDataOrNull()?.id) {
                    IconButton(onClick = {
                        onKebabButtonClicked()
                    }) {
                        Icon(Icons.Rounded.MoreVert, null)
                    }
                }
            })
        },
        bottomBar = {
            if (post != null && meState.needLogin.not()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NetworkImage(
                        url = meState.notNullUserInfo.profilePhotoUrl,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(0.5f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {

                            BasicTextField(
                                modifier = Modifier.weight(1f),
                                value = query,
                                cursorBrush = SolidColor(MaterialTheme.colors.secondary),
                                textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface),
                                onValueChange = { query = it },
                                keyboardActions = KeyboardActions {
                                    viewModel.postComment(
                                        post.id,
                                        query
                                    )
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            if (isPostingComment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colors.secondary
                                )
                            } else {
                                IconButton(
                                    modifier = Modifier.size(24.dp),
                                    onClick = { viewModel.postComment(post.id, query) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Send,
                                        contentDescription = "보내기",
                                        tint = MaterialTheme.colors.onBackground.copy(0.5f)
                                    )
                                }
                            }

                        }
                    }


                }
            }
        }
    ) {

        if (isDeletingPost) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colors.secondary
            )
        }

        if (state.showDeletePostConfirmDialog) {
            BasicDialog(
                onDismissRequest = {
                    viewModel.setShowDeletePostConfirmDialogState(false)
                },
                title = "정말 삭제하시겠어요??",
                message = "삭제한 게시글은 다시 되돌릴 수 없어요!.",
                positiveButton = {
                    FilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.deletePost(post!!.id)
                            viewModel.setShowDeletePostConfirmDialogState(false) },
                        text = "확인"
                    )
                },
                dialogContentGravity =  DialogContentGravity.Top,
                negativeButton = {
                    FilledButton(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colors.surfaceOver,
                        onClick = { viewModel.setShowDeletePostConfirmDialogState(false) },
                        text = "취소"
                    )
                }
            )
        }
        if (post != null) {
            LazyColumn(
                modifier = Modifier.padding(it),
                content = {
                    item {
                        LargePostCard(
                            post = post,
                            onClicked = { /*TODO*/ },
                            onVoteClicked = {
                                viewModel.requestVote(post.id, it.id)
                            },
                            onScrapClicked = {
                                viewModel.requestScrap(post.id)
                            },
                            onLikeClicked = {
                                viewModel.requestLike(post.id)
                            }) {

                        }
                    }

                    items(comments) { comment ->
                        if (comment != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                NetworkImage(
                                    url = comment.author.profilePhotoUrl,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Column {
                                    Text(
                                        comment.author.nickname,
                                        fontSize = 10.sp,
                                        color = SabotenColors.grey400
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        comment.text,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        comment.createdAt.asDurationStringFromNow(),
                                        fontSize = 10.sp,
                                        color = SabotenColors.grey400
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            )


        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
            }

        }
    }

}