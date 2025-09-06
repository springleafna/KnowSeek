package com.springleaf.knowseek.constans;

/**
 * AI 对话相关 Redis Key
 */
public final class ChatRedisKeyConstant {

    private ChatRedisKeyConstant() {}

    public static final String CONVERSATION_KEY_PREFIX = "conversation:user:%s:%s";
    public static final String CONVERSATION_META_KEY_PREFIX = "conversation:meta:user:%s:%s";
    public static final String USER_CONVERSATIONS_KEY_PREFIX = "user:conversations:%s";
}
