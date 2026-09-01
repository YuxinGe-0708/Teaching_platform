package com.teach.user.security;

/** 请求线程内保存当前身份（由 AuthInterceptor 写入、afterCompletion 清除）。 */
public final class IdentityContext {

    private static final ThreadLocal<UserIdentity> CURRENT = new ThreadLocal<>();

    private IdentityContext() {
    }

    public static void set(UserIdentity identity) {
        CURRENT.set(identity);
    }

    public static UserIdentity get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Long requireUserId() {
        UserIdentity identity = CURRENT.get();
        if (identity == null) throw new IllegalStateException("未登录");
        return identity.getUserId();
    }

    public static String requireRole() {
        UserIdentity identity = CURRENT.get();
        if (identity == null) throw new IllegalStateException("未登录");
        return identity.getRole();
    }
}
