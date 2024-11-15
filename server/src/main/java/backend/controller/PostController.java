package backend.controller;

import backend.controller.annotation.Version1RestController;
import backend.controller.dto.*;
import backend.controller.swagger.response.PostNotFoundResponse;
import backend.controller.swagger.response.PostVoteNotFound;
import backend.controller.swagger.response.UnauthorizedResponse;
import backend.jwt.SecurityUtil;
import backend.service.CategoryService;
import backend.service.UserService;
import backend.service.comment.CommentService;
import backend.service.post.*;
import backend.service.user.VoteSelectService;
import common.message.PostResponseMessage;
import common.model.request.Duration;
import common.model.request.post.VoteSelectRequest;
import common.model.request.post.create.PostCreateRequest;
import common.model.request.post.update.PostUpdateRequest;
import common.model.reseponse.ApiResponse;
import common.model.reseponse.PagingResponse;
import common.model.reseponse.category.CategoryResponse;
import common.model.reseponse.post.PostCountResponse;
import common.model.reseponse.post.PostResponse;
import common.model.reseponse.post.VoteResponse;
import common.model.reseponse.post.create.PostCreatedResponse;
import common.model.reseponse.post.read.PostReadResponse;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Version1RestController
@RequiredArgsConstructor
// TODO : PostController에 너무 많은 기능들이 들어있다고 생각함. 따라서 다른 Controller 생성을 하여 분리가 필요.
class PostController {
    private final PostService postService;
    private final UserService userService;
    private final VoteService voteService;
    private final VoteSelectService voteSelectService;
    private final CategoryService categoryService;
    private final CategoryInPostService categoryInPostService;
    private final PostLikeService postLikeService;
    private final PostScrapService postScrapService;
    private final CommentService commentService;


    // 로그인 안된 사용자면 null 반환
    private UserDto getUser() {
        Long userId = SecurityUtil.getCurrentUserId();
        if(userId != null)
            return userService.findUserEntity(userId);

        return null;
    }

    @ApiOperation(value = "특정 게시물 조회", notes = "특정 게시물을 조회하여 모든 정보를 Response로 얻습니다.")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class)
    })
    @GetMapping("/post/{id}")
    public ApiResponse<PostResponse> getPost(@PathVariable Long id) {
        UserDto userDto = getUser();
        PostDto postDto = postService.findPost(id);
        postService.updateView(postDto.getPostId());

        List<VoteResponse> votes = voteService.findVotes(postDto.getPostId());
        List<CategoryResponse> categories = categoryInPostService.findCategoriesInPost(postDto.getPostId());

        Long userId = userDto != null ? userDto.getUserId() : null;
        Long voteResult = findVoteResult(userId, postDto.getPostId());
        boolean isLike = postLikeService.findPostIsLike(userId, postDto.getPostId());
        boolean isScrap = postScrapService.findPostIsScrap(userId, postDto.getPostId());

        // TODO : 매개변수가 5개나 넣어지는데 어떻게 못줄이나....
        // 방법 1 : 외부 테이블 정보들을 그냥 post 테이블 정보로 저장함. (다른 프로젝트보면 이런식으로 되어있는듯?)
        // 방법 2 : 그냥 이대로 씀
        // 방법 3 : 클린코드 알려줘요 ~~
        PostResponse response = postDto.toResponse(votes, categories, voteResult, isLike, isScrap);
        return ApiResponse.withMessage(response, PostResponseMessage.POST_FIND_ONE);
    }

    private Long findVoteResult(Long userId, Long postId) {
        return voteSelectService.findVoteSelectResult(userId, postId);
    }

    @ApiOperation(value = "게시물 등록 (사용자 인증 필요)", notes = "사용자가 게시물 작성하여 등록합니다.")
    @PostMapping("/post")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
    })
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PostCreatedResponse> createPost(@RequestBody PostCreateRequest postCreateRequest) {
        UserDto userDto = getUser();

        List<CategoryDto> categoriesDto = categoryService.getCategories(postCreateRequest.getCategoryIds());
        PostDto postDto= postService.create(postCreateRequest.getText(), userDto);
        List<VoteResponse> votes = voteService.saveVotes(postCreateRequest.getVoteTopics(), postDto);
        List<CategoryResponse> categories = categoryInPostService.saveCategoriesInPost(categoriesDto, postDto);

        PostCreatedResponse response = postDto.toCreatedResponse(votes, categories);
        return ApiResponse.withMessage(response, PostResponseMessage.POST_CREATED);
    }

    @ApiOperation(value = "내가 쓴 게시물 조회 (사용자 인증 필요)", notes = "로그인한 사용자 자기자신의 게시물을 조회합니다.")
    @GetMapping("/post/my")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
    })
    public ApiResponse<PagingResponse<PostReadResponse>> getUserPost(Pageable pageable){
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;
        Page<PostReadResponse> postPage = postService.getUserPost(userId, pageable)
                .map(postDto ->
                    makeReadResponse(postDto, userDto)
                );

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> myPostPage = new PagingResponse<>(postPage.getContent(), nextPage, postPage.getTotalPages());
        return ApiResponse.withMessage(myPostPage, PostResponseMessage.POST_FIND_USER);
    }

    @ApiOperation(value = "전체 게시물 리스트 조회",
            notes = "기본적으로는 전체 게시물 리스트를 조회하지만 특정 카테고리에 해당하는 게시물 리스트를 조회할 수도 있습니다.")
    @ApiImplicitParam(name="categoryId", value="카테고리 id 입니다. (필터 역할)")
    @GetMapping("/post")
    public ApiResponse<PagingResponse<PostReadResponse>> getPostList(@RequestParam Long categoryId,
                                                   @PageableDefault(sort = "registDate", direction = Direction.DESC) Pageable pageable){
        Page<PostReadResponse> postPage;
        UserDto userDto = getUser();

        if(categoryId == 10){ // 전체 게시물 조회
            postPage = postService.findAllPageable(pageable)
                    .map(postDto -> makeReadResponse(postDto, userDto));
        }
        else {
            CategoryDto categoryDto = categoryService.findCategory(categoryId);
            postPage = categoryInPostService.findCategoryInPostPageByCategoryId(categoryDto.getCategoryId(), pageable)
                    .map(categoryInPostDto -> makeReadResponse(categoryInPostDto.getPost(), userDto));
        }

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> myPostPage = new PagingResponse<>(postPage.getContent(), nextPage, postPage.getTotalPages());
        return ApiResponse.withMessage(myPostPage, PostResponseMessage.POST_FIND_ALL);
    }

    @ApiOperation(value = "전체 게시물 조회(최신순)")
    @GetMapping("/post/recent")
    public ApiResponse<PagingResponse<PostReadResponse>> getPostPageOrderedByRegistDate(
            @PageableDefault(sort = "registDate", direction = Direction.DESC) Pageable pageable) {
        UserDto userDto = getUser();
        Page<PostReadResponse> postPage = postService.findAllPageable(pageable).map(postDto ->
                makeReadResponse(postDto, userDto));

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> myPostPage = new PagingResponse<>(postPage.getContent(), nextPage, postPage.getTotalPages());
        return ApiResponse.withMessage(myPostPage, PostResponseMessage.POST_FIND_ALL_ORDERED_BY_REGIST_DATE);
    }

    @ApiOperation(value = "전체 게시물 조회(좋아요순)")
    @GetMapping("/post/liked")
    public ApiResponse<PagingResponse<PostReadResponse>> getPostPageOrderedByLikedCount(
            @PageableDefault(sort = "postLikeCount", direction = Direction.DESC) Pageable pageable) {

        Page<PostReadResponse> postPage = postService.findAllPageable(pageable).map(postDto ->
            makeReadResponse(postDto, getUser())
        );

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> myPostPage = new PagingResponse<>(postPage.getContent(), nextPage, postPage.getTotalPages());
        return ApiResponse.withMessage(myPostPage, PostResponseMessage.POST_FIND_ALL_ORDERED_BY_LIKED_COUNT);
    }

    @ApiOperation(value = "전체 게시물 조회(좋아요순 5개)")
    @GetMapping("/post/liked/list")
    public ApiResponse<List<PostReadResponse>> getPostListOrderedByLikedCount() {
        List<PostDto> postDtoList = postService.findAllOrderedBySortItemList("postLikeCount");
        List<PostReadResponse> myPostList = postDtoList.stream().map(postDto ->
            makeReadResponse(postDto, getUser())
        ).limit(5).toList();
        return ApiResponse.withMessage(myPostList, PostResponseMessage.POST_FIND_FIVE_ORDERED_BY_LIKED_COUNT);
    }

    @ApiOperation(value = "게시물 검색",
            notes = "사용자가 검색어를 입력하면, 검색어가 들어간 게시물을 조회할 수 있습니다.")
    @GetMapping("/post/search")
    public ApiResponse<PagingResponse<PostReadResponse>> getSearchPost(@RequestParam(required = false) String searchText, @PageableDefault Pageable pageable) {
        Page<PostReadResponse> postPage = postService.searchPost(searchText, pageable).map(postDto ->
                makeReadResponse(postDto, getUser())
        );

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> myPostPage = new PagingResponse<>(postPage.getContent(), nextPage, postPage.getTotalPages());
        return ApiResponse.withMessage(myPostPage, PostResponseMessage.POST_FIND_ALL);
    }

    @ApiOperation(value = "게시물 검색 개수 조회",
            notes = "사용자가 검색어를 입력하면, 검색어가 들어간 게시물에 대한 전체 개수를 조회합니다.")
    @GetMapping("/post/search/count")
    public ApiResponse<Long> getSearchPostCount(@RequestParam(required = false) String searchText) {
        Long postCount = postService.searchPostCount(searchText);
        return ApiResponse.withMessage(postCount, PostResponseMessage.POST_SEARCH_ALL_COUNT);
    }

    @ApiOperation(value = "게시물 수정 (사용자 인증 필요)", notes = "사용자가 게시물 수정하여 갱신합니다.")
    @PutMapping("/post")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class),
    })
    public ApiResponse<PostResponse> updatePost(@RequestBody PostUpdateRequest postUpdateRequest) {
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;
        PostDto postDto = postService.updatePost(userId, postUpdateRequest.getId(), postUpdateRequest.getText());

        List<CategoryDto> categoriesDto = categoryService.findCategories(postUpdateRequest.getCategoryIds());
        List<CategoryResponse> categories = categoryInPostService.update(postDto, categoriesDto);

        List<VoteResponse> votes = voteService.update(postUpdateRequest.getVoteTopics(), postDto.getPostId());

        PostResponse response = postDto.toResponse(votes, categories, null, null, null);
        return ApiResponse.withMessage(response, PostResponseMessage.POST_UPDATED);
    }

    @ApiOperation(value = "게시물 삭제 (사용자 인증 필요)", notes = "사용자가 게시물을 삭제합니다.")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class),
    })
    @DeleteMapping("/post/{id}")
    public ApiResponse<?> removePost(@PathVariable Long id) {
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;
        postService.deletePost(userId, id);

        return ApiResponse.withMessage("", PostResponseMessage.POST_DELETED);
    }

    @ApiOperation(value = "게시물 좋아요 등록 (사용자 인증 필요)", notes = "사용자가 게시물을 좋아요 합니다.")
    @PostMapping("/post/{id}/like")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class),
    })
    public ApiResponse<PostResponse> postLike(@PathVariable Long id) {
        UserDto userDto = getUser();
        PostDto postDto = postService.findPost(id);

        boolean isLike = postLikeService.triggerPostLike(userDto, postDto);
        List<VoteResponse> votes = voteService.findVotes(postDto.getPostId());
        List<CategoryResponse> categories = categoryInPostService.findCategoriesInPost(postDto.getPostId());

        Long userId = userDto != null ? userDto.getUserId() : null;
        Long voteResult = voteSelectService.findVoteSelectResult(userId, postDto.getPostId());
        boolean isScrap = postScrapService.findPostIsScrap(userId, postDto.getPostId());

        if(isLike) {
            postService.increasePostLike(id);
            postDto.setPostLikeCount(postDto.getPostLikeCount() + 1);
            PostResponse postLikeResponse = postDto.toResponse(votes, categories, voteResult, isLike, isScrap);
            return ApiResponse.withMessage(postLikeResponse, PostResponseMessage.POST_LIKE_SUCCESS);
        }
        else {
            postService.decreasePostLike(id);
            postDto.setPostLikeCount(postDto.getPostLikeCount() - 1);
            PostResponse postLikeResponse = postDto.toResponse(votes, categories, voteResult, isLike, isScrap);
            return ApiResponse.withMessage(postLikeResponse, PostResponseMessage.POST_UNLIKE_SUCCESS);
        }
    }

    @ApiOperation(value = "게시물 스크랩 등록 및 취소 (사용자 인증 필요)", notes = "사용자가 게시물을 스크랩 등록 및 취소합니다.")
    @PostMapping("/post/{id}/scrap")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class),
    })
    public ApiResponse<PostResponse> postScrap(@PathVariable Long id) {
        UserDto userDto = getUser();
        PostDto postDto = postService.findPost(id);

        boolean isScrap = postScrapService.triggerPostScrap(userDto, postDto);
        List<VoteResponse> votes = voteService.findVotes(postDto.getPostId());
        List<CategoryResponse> categories = categoryInPostService.findCategoriesInPost(postDto.getPostId());

        Long userId = userDto != null ? userDto.getUserId() : null;
        Long voteResult = voteSelectService.findVoteSelectResult(userId, postDto.getPostId());
        boolean isLike = postLikeService.findPostIsLike(userId, postDto.getPostId());

        PostResponse postScrapResponse = postDto.toResponse(votes, categories, voteResult, isLike, isScrap);

        if(isScrap) {
            return ApiResponse.withMessage(postScrapResponse, PostResponseMessage.POST_SCRAP_SUCCESS);
        }
        else {
            return ApiResponse.withMessage(postScrapResponse, PostResponseMessage.POST_CANCEL_SCRAP_SUCCESS);
        }
    }

    @ApiOperation(value = "게시물 스크랩 조회 (사용자 인증 필요)", notes = "사용자가 스크랩한 게시물들을 조회합니다.")
    @GetMapping("/post/my/scrap")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
    })
    public ApiResponse<PagingResponse<PostReadResponse>> getPostScrap(Pageable pageable) {
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;

        Page<PostScrapDto> postScrapesDto = postScrapService.getUserScrap(userId, pageable);
        Page<PostReadResponse> myPostScrap = postScrapesDto.map(postScrapDto -> {
            PostDto postDto = postScrapDto.getPost();
            return makeReadResponse(postDto, userDto);
        });

        Long nextPage = myPostScrap.isLast() ? null : (long) myPostScrap.getNumber() + 2;
        PagingResponse<PostReadResponse> myPostScrapPage = new PagingResponse<>(myPostScrap.getContent(), nextPage, myPostScrap.getTotalPages());
        return ApiResponse.withMessage(myPostScrapPage, PostResponseMessage.POST_SCRAP_FIND_SUCCESS);
    }
    
    @ApiOperation(value = "뜨거운 고민거리 API 입니다.", notes = "카테고리 와 게시물 기간에 따라 게시물들을 조회합니다.")
    @ApiImplicitParams({
        @ApiImplicitParam(name="categoryId", value="카테고리 id 입니다."),
        @ApiImplicitParam(name="duration", value="게시물 기간입니다. DAY, WEEK, MONTH, ALL 이 있습니다.")
    })
    @GetMapping("/post/debate")
    public ApiResponse<PagingResponse<PostReadResponse>> getHotDebatePostList(@RequestParam(required = true) Long categoryId,
                                                              @RequestParam(required = true) Duration duration,
                                                              @PageableDefault Pageable pageable) {
        Page<PostDto> postPage = null;
        UserDto userDto = getUser();

        if(categoryId == 10){
            postPage = postService.findAllHotDebatePost(duration, pageable);
        }
        else {
            categoryService.findCategory(categoryId);
            Page<CategoryInPostDto> categoryInPostPage = categoryInPostService.findHotDebateCategoryInPost(categoryId, duration, pageable);
            postPage = categoryInPostPage.map(CategoryInPostDto::getPost);
        }

        Page<PostReadResponse> postPageResponse = postPage.map(postDto ->
                makeReadResponse(postDto, userDto)
        );

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> hotPostPage = new PagingResponse<>(postPageResponse.getContent(), nextPage, postPageResponse.getTotalPages());
        return ApiResponse.withMessage(hotPostPage, PostResponseMessage.POST_HOT_DEBATE_FIND_ALL);
    }

    @ApiOperation(value = "Hot 게시판 조회", notes = "추천수가 높은 게시물들을 조회합니다.")
    @GetMapping("/post/hot")
    public ApiResponse<PagingResponse<PostReadResponse>> getHotPostList(@PageableDefault Pageable pageable) {

        UserDto userDto = getUser();
        Page<PostReadResponse>  postPage = postService.findAllHotPost(pageable).map(postDto ->
                makeReadResponse(postDto, userDto)
        );

        Long nextPage = postPage.isLast() ? null : (long) postPage.getNumber() + 2;
        PagingResponse<PostReadResponse> myHotPostPage = new PagingResponse<>(postPage.getContent(), nextPage, postPage.getTotalPages());
        return ApiResponse.withMessage(myHotPostPage, PostResponseMessage.POST_HOT_FIND_ALL);
    }

    @ApiOperation(value = "게시물 투표 (사용자 인증 필요)", notes = "사용자가 게시물을 투표합니다.")
    @PostMapping("/post/{id}/vote")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostNotFoundResponse.class),
            @io.swagger.annotations.ApiResponse(code = 404, message = "", response = PostVoteNotFound.class),
    })
    public ApiResponse<PostResponse> votePost(@RequestBody VoteSelectRequest voteSelectRequest, @PathVariable Long id) {
        UserDto userDto = getUser();

        PostDto postDto = postService.findPost(id);
        Long oldVoteSelectResult = voteSelectService.checkExistVoteSelect(userDto.getUserId(), id, voteSelectRequest.getId());
        boolean isIncrease = voteService.increaseVoteCount(oldVoteSelectResult, voteSelectRequest.getId(), id);
        voteSelectService.saveVoteSelect(postDto, userDto, voteSelectRequest.getId(), isIncrease);

        List<VoteResponse> votes = voteService.findVotes(postDto.getPostId());
        List<CategoryResponse> categories = categoryInPostService.findCategoriesInPost(postDto.getPostId());

        Long userId = userDto != null ? userDto.getUserId() : null;
        Long voteResult = voteSelectService.findVoteSelectResult(userId, postDto.getPostId());
        boolean isLike = postLikeService.findPostIsLike(userId, postDto.getPostId());
        boolean isScrap = postScrapService.findPostIsScrap(userId, postDto.getPostId());

        PostResponse voteResponse = postDto.toResponse(votes, categories, voteResult, isLike, isScrap);
        return ApiResponse.withMessage(voteResponse, PostResponseMessage.POST_VOTE_SUCCESS);
    }

    @ApiOperation(value = "자기가 투표한 게시물 조회 (사용자 인증 필요)", notes = "사용자가 투표했던 게시물들을 조회합니다.")
    @GetMapping("/post/my/voted")
    @ApiResponses({
            @io.swagger.annotations.ApiResponse(code = 401, message = "", response = UnauthorizedResponse.class),
    })
    public ApiResponse<PagingResponse<PostReadResponse>> getPostVoted(Pageable pageable) {
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;

        Page<PostDto> postsDto = voteSelectService.findPostVoted(userId, pageable);
        Page<PostReadResponse> myPostVoted = postsDto.map(postDto ->
                makeReadResponse(postDto, userDto)
        );

        Long nextPage = myPostVoted.isLast() ? null : (long) myPostVoted.getNumber() + 2;
        PagingResponse<PostReadResponse> postPage = new PagingResponse<>(myPostVoted.getContent(), nextPage, myPostVoted.getTotalPages());
        return ApiResponse.withMessage(postPage, PostResponseMessage.POST_VOTED_FIND_SUCCESS);
    }

    @ApiOperation(value = "마이페이지 게시글 카운트")
    @GetMapping("/mypage")
    public ApiResponse<PostCountResponse> getPostCount() {
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;

        Long myPostCount = postService.countByUserId(userId);
        Long votedCount = voteSelectService.countByUserId(userId);
        Long scrapedCount = postScrapService.countByUserId(userId);

        PostCountResponse postCountResponse = new PostCountResponse(myPostCount, votedCount, scrapedCount);
        return ApiResponse.withMessage(postCountResponse, PostResponseMessage.POST_COUNT_BY_USER_ID);
    }

    @GetMapping("mypage/comment")
    public ApiResponse<List<PostReadResponse>> getPostCommented() {
        UserDto userDto = getUser();
        Long userId = userDto != null ? userDto.getUserId() : null;

        List<Long> postIdList = commentService.findPostIdByUserId(userId);
        List<PostDto> postDtoList = postIdList.stream().map(postService::findPost).toList();
        List<PostReadResponse> postResponseList = postDtoList.stream().map(postDto ->
                makeReadResponse(postDto, userDto)
        ).toList();
        return ApiResponse.withMessage(postResponseList, PostResponseMessage.COMMENTED_POST_FIND_ALL_BY_USERID);
    }
    private PostReadResponse makeReadResponse(PostDto postDto, UserDto userDto) {
        List<CategoryResponse> categories = categoryInPostService.findCategoriesInPost(postDto.getPostId());

        if(userDto == null) {
            return postDto.toReadResponse(voteService.findVotes(postDto.getPostId()), categories, null, false, false);
        }
        Long voteResult = findVoteResult(userDto.getUserId(), postDto.getPostId());
        Boolean isScraped = postScrapService.findPostIsScrap(userDto.getUserId(), postDto.getPostId());
        Boolean isLiked = postLikeService.findPostIsLike(userDto.getUserId(), postDto.getPostId());

        return postDto.toReadResponse(voteService.findVotes(postDto.getPostId()), categories, voteResult, isScraped, isLiked);
    }
}
