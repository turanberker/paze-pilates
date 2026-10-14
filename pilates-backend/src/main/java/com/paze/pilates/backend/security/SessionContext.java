package com.paze.pilates.backend.security;

import java.util.Optional;
import org.springframework.stereotype.Component;

/** Masaüstü uygulamada tek oturum vardır; SwingWorker iş parçacıkları da okuyabilsin diye volatile. */
@Component
public class SessionContext {

    private volatile CurrentUser user;

    public Optional<CurrentUser> get() {
        return Optional.ofNullable(user);
    }

    public void set(CurrentUser user) {
        this.user = user;
    }

    public void clear() {
        this.user = null;
    }
}
