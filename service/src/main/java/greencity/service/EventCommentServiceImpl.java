package greencity.service;

import greencity.client.RestClient;
import greencity.constant.ErrorMessage;
import greencity.dto.PageableDto;
import greencity.dto.eventcomment.EventCommentRequestDto;
import greencity.dto.eventcomment.EventCommentResponseDto;
import greencity.dto.user.UserVO;
import greencity.entity.EventComment;
import greencity.entity.User;
import greencity.entity.event.Event;
import greencity.enums.CommentStatus;
import greencity.exception.exceptions.NotFoundException;
import greencity.exception.exceptions.UserHasNoPermissionToAccessException;
import greencity.repository.EventCommentRepo;
import greencity.repository.EventRepo;
import greencity.repository.UserRepo;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EventCommentServiceImpl implements EventCommentService {
    private final EventRepo eventRepo;
    private final EventCommentRepo eventCommentRepo;
    private final RestClient restClient;
    private final UserRepo userRepo;
    private ModelMapper modelMapper;

    /**
     * Method to save {@link EventComment}.
     *
     * @param eventId                   id of {@link Event} to
     *                                    which we save comment.
     * @param requestDto dto with
     *                                    {@link EventComment}
     *                                    text, parentCommentId.
     * @param user                      {@link User} that saves the comment.
     * @return {@link EventCommentResponseDto} instance.
     */
    @Override
    public EventCommentResponseDto save(Long eventId, EventCommentRequestDto requestDto, UserVO user) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId));
        EventComment eventComment = modelMapper.map(requestDto, EventComment.class);
        eventComment.setEvent(event);
        eventComment.setUser(modelMapper.map(user, User.class));
        Set<User> mentionedUsers = new HashSet<>();
        if (requestDto.getText().contains("@") || requestDto.getText().contains("#")) {
            String[] textByWord = requestDto.getText().split(" ");
            Pattern p1 = Pattern.compile("@\\w+");
            Pattern p2 = Pattern.compile("#\\w+");
            List<String> usernames = Arrays.stream(textByWord)
                    .filter(word -> {
                Matcher m1 = p1.matcher(word);
                Matcher m2 = p2.matcher(word);
                return m1.matches() || m2.matches();
            })
                    .map(username -> username.substring(1))
                    .toList();
            mentionedUsers = usernames.stream().map(userRepo::findByName)
                    .filter(Optional::isPresent)
                    .map(Optional::get).collect(Collectors.toSet());
        }
        eventComment.setMentionedUsers(mentionedUsers);
        eventComment.setStatus(CommentStatus.ORIGINAL);

        eventComment = eventCommentRepo.save(eventComment);
        return modelMapper.map(eventComment, EventCommentResponseDto.class);
    }

    /**
     * Method to count not deleted comments to certain
     * {@link Event}.
     *
     * @param eventId to specify {@link Event}
     * @return amount of comments
     */
    @Override
    public int countOfComments(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId));
        return eventCommentRepo.countByEvent(event);
    }

    /**
     * Method returns all comments to certain event specified by eventId.
     *
     * @param userVO    current {@link User}
     * @param eventId specifies {@link Event} to which we
     *                  search for comments
     * @return all comments to certain event specified by eventId.
     */
    @Override
    public PageableDto<EventCommentResponseDto> getAllEventComments(Pageable pageable, Long eventId, UserVO userVO) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_NOT_FOUND_BY_ID + eventId));
        Page<EventComment> pages = eventCommentRepo.findAllByEventOrderByCreatedDateDesc(event, pageable);
        List<EventCommentResponseDto> eventComments = pages.stream()
                .map(eventComment -> modelMapper.map(eventComment, EventCommentResponseDto.class))
                .toList();
        return new PageableDto<>(
                eventComments,
                pages.getTotalElements(),
                pages.getPageable().getPageNumber(),
                pages.getTotalPages());
    }

    /**
     * Method set 'DELETED' for field 'status' of the comment {@link EventComment} by id.
     *
     * @param eventCommentId specifies {@link EventComment} to which we search for comments.
     */
    @Transactional
    @Override
    public String delete(Long eventCommentId, String email) {
        EventComment eventComment = eventCommentRepo.findByIdAndStatusNot(eventCommentId, CommentStatus.DELETED)
                .orElseThrow(() -> new NotFoundException(ErrorMessage.EVENT_COMMENT_NOT_FOUND_BY_ID + eventCommentId));

        UserVO currentUser = restClient.findByEmail(email);

        if (!currentUser.getId().equals(eventComment.getUser().getId())) {
            throw new UserHasNoPermissionToAccessException(ErrorMessage.USER_HAS_NO_PERMISSION);
        }

        eventComment.setStatus(CommentStatus.DELETED);
        if (eventComment.getComments() != null) {
            eventComment.getComments()
                    .forEach(comment -> comment.setStatus(CommentStatus.DELETED));
        }

        eventCommentRepo.save(eventComment);
        return "Comment deleted successfully";
    }
}
