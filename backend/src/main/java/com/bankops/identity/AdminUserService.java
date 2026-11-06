package com.bankops.identity;

import com.bankops.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository users;

    public AdminUserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummary> page(int page, int size) {
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int boundedPage = Math.max(0, page);
        var pageable = PageRequest.of(boundedPage, boundedSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        return PageResponse.of(users.findAll(pageable), UserSummary::from);
    }
}
