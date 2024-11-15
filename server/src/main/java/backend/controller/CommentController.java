package backend.controller;

import backend.controller.annotation.Version1RestController;
import backend.controller.dto.CommentDto;
import backend.controller.dto.PostDto;
import backend.controller.dto.UserDto;
import backend.controller.swagger.response.*;
import backend.jwt.SecurityUtil;
import backend.service.UserService;
import backend.service.comment.CommentService;
import backend.service.post.PostService;
import backend.service.user.VoteSelectService;
import common.message.CommentResponseMessage;
import common.model.request.comment.CommentCreateRequest;
import common.model.reseponse.ApiResponse;
import common.model.reseponse.PagingResponse;
import common.model.reseponse.comment.CommentResponse;
import common.model.reseponse.post.read.PostReadResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Version1RestController
@RequiredArgsConstructor
public class  CommentController {

    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;
    private final VoteSelectService voteSelectService;

    // 로그인 안된 사용자면 null 반환
    private UserDto getUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if(userId != null)
            return userService.findUserEntity(userId);

        return null;
    }

    @ApiOperation(value = "댓글작성 API (사용자 인증 필요)", notes = "특정 포스트에 댓글을 작성하는 API입니다.")
    @PostMapping("post/{postId}/comment")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 400, message = "", response = CommentIsNullResponse.class),
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class)
    })
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(@PathVariable Long postId, @RequestBody CommentCreateRequest commentCreateRequest){
        UserDto userDto = getUser();
        PostDto postDto = postService.findPost(postId);

        Long voteSelectResult = voteSelectService.findVoteSelectResult(userDto.getUserId(), postDto.getPostId());
        String text = commentCreateRequest.getText();

        CommentDto commentDto = commentService.create(userDto, postDto, text);

        return ApiResponse.withMessage(commentDto.toCommentResponse(userDto, voteSelectResult),
                CommentResponseMessage.COMMENT_CREATED);
    }

    @ApiOperation(value = "포스트별 댓글조회 API", notes = "특정 포스트에 달린 댓글을 모두 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class)
    })
    @GetMapping("post/{postId}/comment")
    public ApiResponse<PagingResponse<CommentResponse>> getAllCommentsByPost(@PathVariable Long postId, @PageableDefault Pageable pageable){
        PostDto postDto = postService.findPost(postId);
        Page<CommentDto> commentPage = commentService.getAllCommentsByPost(postDto.getPostId(), pageable);

        Page<CommentResponse> commentResponses = commentPage.map(commentDto ->
                commentDto.toCommentResponse(commentDto.getUser(),
                        voteSelectService.findVoteSelectResult(commentDto.getUser().getUserId(), postDto.getPostId()))
        );

        Long nextPage = commentResponses.isLast() ? null : (long) commentResponses.getNumber() + 2;
        PagingResponse<CommentResponse> commentPaging = new PagingResponse<>(commentResponses.getContent(), nextPage, commentResponses.getTotalPages());
        return ApiResponse.withMessage(commentPaging,CommentResponseMessage.COMMENT_FIND_ALL);
    }

    // TODO : URL이 /post/my/comment 가 어울리지 않는지?
    @ApiOperation(value = "유저별 댓글조회 API (사용자 인증 필요)", notes = "로그인 된 유저가 단 댓글들을 모두 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class)
    })
    @GetMapping("post/comment")
    public ApiResponse<PagingResponse<CommentResponse>> getAllCommentsByUser(@PageableDefault Pageable pageable){
        UserDto userDto = getUser();
        Page<CommentDto> commentPage = commentService.getAllCommentsByUser(userDto.getUserId(), pageable);

        Page<CommentResponse> commentResponses = commentPage.map(commentDto ->
                commentDto.toCommentResponse(commentDto.getUser(),
                        voteSelectService.findVoteSelectResult(commentDto.getUser().getUserId(), commentDto.getPost().getPostId()))
        );

        Long nextPage = commentResponses.isLast() ? null : (long) commentResponses.getNumber() + 2;
        PagingResponse<CommentResponse> commentPaging = new PagingResponse<>(commentResponses.getContent(), nextPage, commentResponses.getTotalPages());
        return ApiResponse.withMessage(commentPaging,CommentResponseMessage.COMMENT_FIND_USER);
    }

    @ApiOperation(value = "댓글 삭제 API (사용자 인증 필요)", notes = "본인 댓글을 삭제 합니다.")
    @DeleteMapping("post/{postId}/comment/{commentId}")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = CommentNotFoundResponse.class)
    })
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> deleteComment(@PathVariable Long postId, @PathVariable Long commentId){
        UserDto userDto = getUser();
        commentService.deleteComment(commentId, postId, userDto);
        return ApiResponse.withMessage(null,CommentResponseMessage.COMMENT_DELETED);
    }
}